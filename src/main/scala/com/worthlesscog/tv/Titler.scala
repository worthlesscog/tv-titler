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

object Op extends Enumeration {
    val download, merge, noop, resize, search, searchAll = Value
}

object Titler {

    val CONFIG = ".titler.cfg"
    val DONE = "Done\n"

    val commas = """(\d{1,2}(?:,\d{1,2})*)""".r
    val range = """(\d{1,2})(?:-(\d{1,2})?)?""".r

    val databases = Map(Tmdb.databaseId -> Tmdb, Tvdb.databaseId -> Tvdb)
    val home = Paths.get(System.getProperty("user.home"))
    val players = Map(Mede8er.playerId -> Mede8er)

    var db: TvDatabase = Tmdb
    var db2: TvDatabase = Tvdb
    var dir = new File(".")
    var id = ""
    var id2 = ""
    var lang = "en"
    var op = Op.noop
    var player: MediaPlayer = Mede8er
    var resize = ""
    var search = ""
    var seasons: Option[Set[Int]] = None

    // val args1 = Array("-search", "Penny")
    // val args2 = Array("-target", "E:\\tv", "54671")
    // val args3 = Array("-target", "E:\\tv", "54671", "-seasons", "3-")

    // val args1 = Array("-search", "Penny", "-db", "tvdb")
    // val args2 = Array("-target", "E:\\tv", "-db", "tvdb", "265766")
    // val args3 = Array("-target", "E:\\tv", "-db", "tvdb", "265766", "-seasons", "2-")
    // val args4 = Array("-target", "E:\\tv", "-db", "tvdb", "penny-dreadful")

    // val args1 = Array("-searchall", "Penny")
    // val args1 = Array("-searchall", "Mirror")

    // val args1 = Array("-target", "E:\\tv", "-merge", "tmdb", "54671", "tvdb", "penny-dreadful")

    def main(args: Array[String]): Unit = {
        val status = for {
            _ <- parseArgs(args toList)
            status <- run
        } yield status
        info(status.merge)
    }

    private def parseArgs(args: List[String]): Maybe[Boolean] =
        if (args isEmpty) {
            if (op == Op.noop)
                "??\n" |> asLeft
            else
                true |> asRight
        } else args match {
            case i :: tail if !(i startsWith "-") =>
                id = i
                op = Op.download
                parseArgs(tail)

            case "-db" :: d :: tail =>
                maybeSet(db = _, d) match {
                    case Left(error) => error |> asLeft
                    case _           => parseArgs(tail)
                }

            case "-lang" :: l :: tail =>
                lang = l
                parseArgs(tail)

            case "-merge" :: d1 :: i1 :: d2 :: i2 :: tail =>
                val t = for {
                    _ <- maybeSet(db = _, d1)
                    t <- maybeSet(db2 = _, d2)
                } yield t
                t match {
                    case Left(error) =>
                        error |> asLeft

                    case _ =>
                        id = i1
                        id2 = i2
                        op = Op.merge
                        parseArgs(tail)
                }

            case "-player" :: p :: tail =>
                if (players contains p) {
                    player = players(p)
                    parseArgs(tail)
                } else
                    s"Unknown media player $p\n" |> asLeft

            case "-resize" :: d :: tail =>
                resize = d
                op = Op.resize
                parseArgs(tail)

            case "-search" :: s :: tail =>
                search = s
                op = Op.search
                parseArgs(tail)

            case "-searchall" :: s :: tail =>
                search = s
                op = Op.searchAll
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

    private def run =
        op match {
            case Op.download =>
                for {
                    auth <- loadAuth(home resolve CONFIG toFile)
                    f = download(db, auth, id, seasons, lang)
                    s <- Await.result(f, Duration.Inf)
                    status <- player.generate(s, dir)
                } yield status

            case Op.merge =>
                for {
                    auth <- loadAuth(home resolve CONFIG toFile)
                    f = download(db, auth, id, seasons, lang)
                    f2 = download(db2, auth, id2, seasons, lang)
                    s <- Await.result(f, Duration.Inf)
                    s2 <- Await.result(f2, Duration.Inf)
                    status <- player.merge(s, s2, dir)
                } yield status

            case Op.resize =>
                for {
                    status <- player.resize(new File(resize))
                } yield status

            case Op.search =>
                for {
                    auth <- loadAuth(home resolve CONFIG toFile)
                    f = search(db, search, auth, lang)
                    r <- Await.result(f, Duration.Inf)
                    status <- tabulateSearchResults(db, r)
                } yield status

            case Op.searchAll =>
                for {
                    auth <- loadAuth(home resolve CONFIG toFile)
                    f = search(Tmdb, search, auth, lang)
                    f2 = search(Tvdb, search, auth, lang)
                    r <- Await.result(f, Duration.Inf)
                    r2 <- Await.result(f2, Duration.Inf)
                    status <- tabulateSearchResults(Tmdb, Tvdb, r, r2)
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

    private def download(d: TvDatabase, auth: Map[String, String], id: String, seasons: Option[Set[Int]], lang: String) = Future {
        for {
            token <- authenticate(d, auth)
            series <- d.getTvSeries(id, seasons)(token, lang)
        } yield series
    }

    private def authenticate(d: TvDatabase, auth: Map[String, String]) =
        for {
            credentials <- getCredentials(d, auth)
            token <- d.authenticate(credentials)
        } yield token

    private def getCredentials(d: TvDatabase, auth: Map[String, String]) =
        auth.get(d.databaseId).fold[Maybe[Credentials]] { s"No auth for ${ d.databaseId }\n" |> asLeft } {
            t => Credentials(token = Some(t)) |> asRight
        }

    private def search(d: TvDatabase, search: String, auth: Map[String, String], lang: String) = Future {
        for {
            token <- authenticate(d, auth)
            results <- d.search(search)(token, lang)
        } yield results
    }

    private def tabulateSearchResults(d: TvDatabase, r: Seq[SearchResult]) = {
        val fmt = rjs(genreLen(r) + 3) + "   " +
            ljs(langLen(r) + 3) +
            ljs(airLen(r) + 3) +
            ljs(idLen(r, d.databaseId.length) + 3) +
            "%s\n"

        fmt.format("Genre", "Lang", "Aired", d.databaseId, "Name") |> info
        r.sortBy { _.name } foreach { r =>
            fmt.format(opt(r.genres map commaSeperated), opt(r.language), opt(r.firstAirDate), r.id.toString, r.name) |> info
        }

        DONE |> asRight
    }

    private def genreLen(s: Seq[SearchResult]) = s.flatMap { _.genres map commaSeperated } |> longest max 5
    private def commaSeperated(seq: Seq[_]) = seq mkString ", "
    private def longest[T](s: Seq[T]) = (s map { _.toString length }) |> max
    private def max(s: Seq[Int]) = if (s isEmpty) 0 else s max
    private def langLen(s: Seq[SearchResult]) = s.flatMap { _.language } |> longest max 4
    private def airLen(s: Seq[SearchResult]) = s.flatMap { _.firstAirDate } |> longest max 5
    private def idLen(s: Seq[SearchResult], min: Int) = s.map { _.id } |> longest max min
    private def rjs(width: Int) = "%" + width + "s"
    private def ljs(width: Int) = "%-" + width + "s"
    private def opt[T](t: Option[T]) = t.fold { "" } { _.toString }

    private def tabulateSearchResults(d: TvDatabase, d2: TvDatabase, r: Seq[SearchResult], r2: Seq[SearchResult]) = {
        def interleave(fmt: String, s: Seq[SearchResult], s2: Seq[SearchResult]): Unit =
            if (s2 isEmpty)
                s foreach { r => format(fmt, r, r.id.toString, "") }
            else if (s isEmpty)
                s2 foreach { r => format(fmt, r, "", r.id.toString) }
            else (s.head.name, s2.head.name) match {
                case (n1, n2) if n1 < n2 =>
                    format(fmt, s.head, s.head.id.toString, "")
                    interleave(fmt, s.tail, s2)

                case (n1, n2) if n1 == n2 =>
                    if (s.head.firstAirDate == s2.head.firstAirDate) {
                        format(fmt, s.head, s.head.id.toString, s2.head.id.toString)
                        interleave(fmt, s.tail, s2.tail)
                    } else {
                        format(fmt, s.head, s.head.id.toString, "")
                        interleave(fmt, s.tail, s2)
                    }

                case _ =>
                    format(fmt, s2.head, "", s2.head.id.toString)
                    interleave(fmt, s, s2.tail)
            }

        def format(fmt: String, r: SearchResult, id1: String, id2: String) =
            fmt.format(opt(r.genres map commaSeperated), opt(r.language), opt(r.firstAirDate), id1, id2, r.name) |> info

        val both = r ++ r2
        val fmt = rjs(genreLen(both) + 3) + "   " +
            ljs(langLen(both) + 3) +
            ljs(airLen(both) + 3) +
            ljs(idLen(r, d.databaseId.length) + 3) +
            ljs(idLen(r2, d2.databaseId.length) + 3) +
            "%s\n"

        fmt.format("Genre", "Lang", "Aired", d.databaseId, d2.databaseId, "Name") |> info
        interleave(fmt, r sortBy { _.name }, r2 sortBy { _.name })

        DONE |> asRight
    }

}
