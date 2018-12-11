package com.worthlesscog.tv

import java.io.{File, IOException}
import java.nio.file.Paths

import com.worthlesscog.tv.data._
import com.worthlesscog.tv.mede8er.Mede8er
import com.worthlesscog.tv.tmdb.Tmdb
import com.worthlesscog.tv.tvdb.Tvdb

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source

object Titler {

    val CONFIG = ".titler.cfg"
    val DONE = "Done\n"

    val commas = """(\d{1,2}(?:,\d{1,2})*)""".r
    val range = """(\d{1,2})(?:-(\d{1,2})?)?""".r

    val databases = Map(Tmdb.databaseId -> Tmdb, Tvdb.databaseId -> Tvdb)
    val home = Paths.get(System.getProperty("user.home"))
    val players = Map(Mede8er.playerId -> Mede8er)

    var db1: TvDatabase = Tmdb
    var db2: TvDatabase = Tvdb
    var dir = new File(".")
    var id1 = ""
    var id2 = ""
    var lang = "en"
    var player: MediaPlayer = Mede8er
    var resize = ""
    var search = ""
    var seasons: Option[Set[Int]] = None

    // val args1 = Array("-search", "Penny")
    // val args2 = Array("54671", "-target", "E:\\tv")
    // val args3 = Array("54671", "-target", "E:\\tv", "-seasons", "3-")

    // val args1 = Array("-search", "Jeannie")
    // val args2 = Array("1660", "-target", "E:\\tv")
    // val args2 = Array("1660", "-target", "E:\\tv", "-lang", "de")

    // val args1 = Array("-search", "Chuck")
    // val args2 = Array("1404", "-target", "E:\\tv", "-seasons", "1")

    // val args1 = Array("-db", "tvdb", "-search", "Penny")
    // val args2 = Array("-target", "E:\\tv", "-db", "tvdb", "265766")
    // val args3 = Array("-target", "E:\\tv", "-db", "tvdb", "265766", "-seasons", "2-")
    // val args4 = Array("-target", "E:\\tv", "-db", "tvdb", "penny-dreadful")

    // val args1 = Array("-target", "E:\\tv", "-merge", "tmdb", "54671", "tvdb", "265766")

    def main(args: Array[String]): Unit = {
        val status = for {
            _ <- parseArgs(args toList)
            status <- run
        } yield status
        info(status.merge)
    }

    private def parseArgs(args: List[String]): Maybe[Boolean] =
        if (args isEmpty) {
            if (id1.isEmpty && resize.isEmpty && search.isEmpty)
                "??\n" |> asLeft
            else
                true |> asRight
        } else args match {
            case id :: tail if !(id startsWith "-") =>
                id1 = id
                parseArgs(tail)

            case "-db" :: db :: tail =>
                maybeSet(db1 = _, db) match {
                    case Left(error) => error |> asLeft
                    case _           => parseArgs(tail)
                }

            case "-lang" :: l :: tail =>
                lang = l
                parseArgs(tail)

            case "-merge" :: d1 :: i1 :: d2 :: i2 :: tail =>
                val t = for {
                    _ <- maybeSet(db1 = _, d1)
                    t <- maybeSet(db2 = _, d2)
                } yield t
                t match {
                    case Left(error) => error |> asLeft
                    case _           => id1 = i1; id2 = i2; parseArgs(tail)
                }

            case "-player" :: p :: tail =>
                if (players contains p) {
                    player = players(p)
                    parseArgs(tail)
                } else
                    s"Unknown media player $p\n" |> asLeft

            case "-resize" :: d :: tail =>
                resize = d
                parseArgs(tail)

            case "-search" :: s :: tail =>
                search = s
                parseArgs(tail)

            case ("-season" | "-seasons") :: commas(c) :: tail =>
                seasons = Some(c split ',' map { _ toInt } toSet)
                parseArgs(tail)

            case ("-season" | "-seasons") :: range(from, Optionally(to)) :: tail =>
                val f = from.toInt
                val t = to.fold(99) { _ toInt }
                seasons = Some((f min t) to (f max t) toSet)
                parseArgs(tail)

            case "-target" :: t :: tail =>
                dir = new File(t)
                parseArgs(tail)

            case unknown :: _ =>
                s"$unknown ??\n" |> asLeft
        }

    private def maybeSet(f: TvDatabase => Unit, db: String) =
        databases.get(db).fold[Maybe[Unit]](s"Unknown TV database $db\n" |> asLeft) { f(_) |> asRight }

    private def run = {
        if (id2 nonEmpty)
            for {
                auth <- loadAuth(home resolve CONFIG toFile)
                f1 = download(db1, auth, id1, seasons, lang)
                f2 = download(db2, auth, id2, seasons, lang)
                s1 <- Await.result(f1, Duration.Inf)
                s2 <- Await.result(f2, Duration.Inf)
                status <- player.merge(s1, s2, dir)
            } yield status
        else if (id1 nonEmpty)
            for {
                auth <- loadAuth(home resolve CONFIG toFile)
                f1 = download(db1, auth, id1, seasons, lang)
                s <- Await.result(f1, Duration.Inf)
                status <- player.generate(s, dir)
            } yield status
        else if (resize nonEmpty)
            for {
                status <- player.resize(new File(resize))
            } yield status
        else
            for {
                auth <- loadAuth(home resolve CONFIG toFile)
                f1 = search(db1, search, auth, lang)
                f2 = search(db2, search, auth, lang)
                r1 <- Await.result(f1, Duration.Inf)
                r2 <- Await.result(f2, Duration.Inf)
                status <- tabulateSearchResults(r1, r2)
            } yield status
    }

    private def loadAuth(f: File) =
        try
            using(Source fromFile f) {
                _.getLines.flatMap(keyPair).toMap |> asRight
            }
        catch {
            case x: IOException => leftException(x)
        }

    private def keyPair(s: String) =
        s split ':' match {
            case Array(from, to) => Some(from.trim -> to.trim)
            case _               => None
        }

    private def download(db: TvDatabase, auth: Map[String, String], id: String, seasons: Option[Set[Int]], lang: String) = Future {
        for {
            token <- authenticate(db, auth)
            series <- db.getTvSeries(id, seasons)(token, lang)
        } yield series
    }

    private def authenticate(db: TvDatabase, auth: Map[String, String]) =
        for {
            credentials <- getCredentials(db, auth)
            token <- db.authenticate(credentials)
        } yield token

    private def getCredentials(db: TvDatabase, auth: Map[String, String]) =
        auth.get(db.databaseId).fold[Maybe[Credentials]] { s"No auth for ${ db.databaseId }\n" |> asLeft } {
            t => Credentials(token = Some(t)) |> asRight
        }

    private def search(db: TvDatabase, search: String, auth: Map[String, String], lang: String) = Future {
        for {
            token <- authenticate(db, auth)
            results <- db.search(search)(token, lang)
        } yield results
    }

    private def tabulateSearchResults(r1: Seq[SearchResult], r2: Seq[SearchResult]) = {
        def commas(seq: Seq[_]) = seq mkString ", "
        def ljs(width: Int) = "%-" + width + "s"
        def longest[T](s: Seq[T]) = (s map { _.toString length }) |> max
        def max(s: Seq[Int]) = if (s isEmpty) 0 else s max
        def opt[T](t: Option[T]) = t.fold { "" } { _.toString }
        def rjs(width: Int) = "%" + width + "s"

        val len1 = r1.flatMap { _.genres map commas } |> longest
        val len2 = r1.flatMap { _.language } |> longest
        val len3 = r1.flatMap { _.firstAirDate } |> longest
        val len4 = r1.map { _.id } |> longest
        val fmt = rjs(len1 + 3) + "   " + ljs(len2 + 3) + ljs(len3 + 3) + ljs(len4 + 3) + "%s\n"

        r1.sortBy { _.name } foreach { r =>
            fmt.format(opt(r.genres map commas), opt(r.language), opt(r.firstAirDate), r.id.toString, opt(r.name)) |> info
        }

        DONE |> asRight
    }

}
