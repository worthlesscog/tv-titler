package com.worthlesscog.tv

import java.io.File
import java.nio.file.Paths

import com.worthlesscog.tv.data._
import com.worthlesscog.tv.maze.Maze
import com.worthlesscog.tv.mede8er.Mede8er
import com.worthlesscog.tv.omdb.Omdb
import com.worthlesscog.tv.tmdb.Tmdb
import com.worthlesscog.tv.tvdb.Tvdb

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.Success

object Titler {

    object Op extends Enumeration {
        val merge, noop, resize, search = Value
    }

    val BUSY = " ...\n"
    val CONFIG = ".titler.cfg"
    val DONE = "Done\n"

    val home = Paths.get(System.getProperty("user.home"))

    val commas = """(\d{1,2}(?:,\d{1,2})*)""".r
    val db = """-(\w{4})$""".r
    val range = """(\d{1,2})(?:-(\d{1,2})?)?""".r

    val omdb = new Omdb with ScalajHttpOps
    val maze = new Maze with ScalajHttpOps
    val tmdb = new Tmdb with ScalajHttpOps
    val tvdb = new Tvdb with ScalajHttpOps
    val databases = Map(maze.databaseId -> maze, omdb.databaseId -> omdb, tmdb.databaseId -> tmdb, tvdb.databaseId -> tvdb)

    val mede8er = new Mede8er with ScalajHttpOps
    val players = Map(mede8er.playerId -> mede8er)

    var dbs: Seq[TvDatabase] = Nil
    var dir = new File(".")
    var ids: Seq[String] = Nil
    var lang = "en"
    var op = Op.noop
    var player: MediaPlayer = mede8er
    var resize = ""
    var search = ""
    var seasons: Option[Set[Int]] = None

    // val args1 = Array("-search", "Battlestar")
    // val args1 = Array("-search", "Penny")
    // val args1 = Array("-search", "Mirror")

    // val args2 = Array("-target", "E:\\tv", "-tmdb", "54671")
    // val args3 = Array("-target", "E:\\tv", "-tmdb", "54671", "-seasons", "3-")

    // val args2 = Array("-target", "E:\\tv", "-tvdb", "265766")
    // val args3 = Array("-target", "E:\\tv", "-tvdb", "265766", "-seasons", "2-")
    // val args4 = Array("-target", "E:\\tv", "-tvdb", "penny-dreadful")

    // val args1 = Array("-target", "E:\\tv", "-maze", "16", "-tmdb", "54671", "-tvdb", "penny-dreadful")

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
            case "-lang" :: l :: tail =>
                lang = l
                parseArgs(tail)

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

            case db(dbid) :: k :: tail =>
                maybeAdd(dbid, k) match {
                    case Left(error) =>
                        error |> asLeft

                    case _ =>
                        op = Op.merge
                        parseArgs(tail)
                }

            case unknown :: _ =>
                s"$unknown ??\n" |> asLeft
        }

    private def maybeAdd(d: String, k: String) =
        databases.get(d).fold[Maybe[Unit]](s"Unknown TV database $d\n" |> asLeft) { db =>
            if (!(dbs contains db)) {
                dbs = dbs :+ db
                ids = ids :+ k
            }
            () |> asRight
        }

    private def run =
        op match {
            case Op.merge =>
                val op = if (dbs.size > 1) "Merging " else "Downloading "
                val idsAndKeys = dbs map { _ databaseId } zip ids map { case (d, k) => d + "/" + k } mkString ", "
                op + idsAndKeys + BUSY |> info
                for {
                    auth <- maybeIO { loadAuth(home resolve CONFIG toFile) }
                    f = dbs zip ids map { case (d, k) => download(d, auth, k, seasons, lang) }
                    r = waitDiscardingFailures(f)
                    status <- player.merge(r, dir)
                } yield status

            case Op.resize =>
                for {
                    status <- player.resize(new File(resize))
                } yield status

            case Op.search =>
                val names = databases.map { case (_, d) => d databaseName } mkString ", "
                "Searching " + names + BUSY |> info
                for {
                    auth <- maybeIO { loadAuth(home resolve CONFIG toFile) }
                    f = databases.values.toSeq map { search(_, search, auth, lang) }
                    r = waitDiscardingFailures(f)
                    status <- tabulateSearch(r flatten)
                } yield status
        }

    private def loadAuth(f: File) =
        using(Source fromFile f) {
            _.getLines.flatMap(keyPair).toMap |> asRight
        }

    private def keyPair(s: String) =
        s split ':' match {
            case Array(from, to) => Some(from.trim -> to.trim)
            case _               => None
        }

    private def download(d: TvDatabase, auth: Map[String, String], id: String, seasons: Option[Set[Int]], lang: String) = Future {
        for {
            token <- authenticate(d, auth)
            series <- d.getTvSeries(id, seasons, token, lang)
        } yield series
    }

    private def authenticate(d: TvDatabase, auth: Map[String, String]) =
        for {
            credentials <- getCredentials(d, auth)
            token <- d.authenticate(credentials)
        } yield token

    private def getCredentials(d: TvDatabase, auth: Map[String, String]) =
        auth.get(d databaseId).fold[Maybe[Credentials]] { s"No auth for ${ d databaseId }\n" |> asLeft } {
            t => Credentials(token = Some(t)) |> asRight
        }

    private def search(d: TvDatabase, search: String, auth: Map[String, String], lang: String) = Future {
        for {
            token <- authenticate(d, auth)
            results <- d.search(search, token, lang)
        } yield results
    }

    private def waitDiscardingFailures[T](fs: Seq[Future[Either[String, T]]]) = {
        val s = Future
            .sequence(fs map { _ transform { Success(_) } })
            .map { _ collect { case Success(Right(r)) => r } }

        Await.result(s, Duration.Inf)
    }

    private def tabulateSearch(rs: Seq[SearchResult[_]]) = {
        def longestString(s: Seq[String]) = { s sortBy { -_.length } }.headOption.getOrElse("")

        def tab(fmt: String, ids: Seq[String], rs: Seq[SearchResult[_]]): Unit =
            rs match {
                case h +: t =>
                    val lc = h.name toLowerCase
                    val ms = h +: t.takeWhile { r => (r.name.toLowerCase == lc) && (r.firstAirDate == h.firstAirDate) }
                    val g = { ms flatMap { _.genres map commaSeperated } } |> longestString
                    val l = { ms flatMap { _.language } } |> longestString
                    val a = { ms flatMap { _.firstAirDate } } |> longestString
                    val d = ids map { i => ms.find { _.source == i }.fold("") { _.id toString } }
                    val args = Seq(g, l, a) ++ d ++ Seq(h name)
                    fmt.format(args: _*) |> info
                    tab(fmt, ids, t.drop(ms.size - 1))

                case _ =>
            }

        val s = 3
        val ids = rs.map { _ source }.distinct sorted
        val dbs = ids map { d => s + idLen(rs filter { _.source == d }, d length) |> ljs }
        val fmt = Seq(rjs(genreLen(rs) + s) + (" " * s) + ljs(langLen(rs) + s) + ljs(airLen(rs) + s)) ++ dbs ++ Seq("%s\n") mkString

        val head = Seq("Genre", "Lang", "Aired") ++ ids ++ Seq("Name")
        fmt.format(head: _*) |> info
        tab(fmt, ids, rs sortBy { r => (r.name toLowerCase, r firstAirDate, r source) })

        DONE |> asRight
    }

    private def idLen(s: Seq[SearchResult[_]], min: Int) = s.map { _ id } |> longest max min
    private def longest[T](s: Seq[T]) = (s map { _.toString length }) |> max
    private def max(s: Seq[Int]) = if (s isEmpty) 0 else s max
    private def genreLen(s: Seq[SearchResult[_]]) = s.flatMap { _.genres map commaSeperated } |> longest max 5
    private def commaSeperated(seq: Seq[_]) = seq mkString ", "
    private def langLen(s: Seq[SearchResult[_]]) = s.flatMap { _ language } |> longest max 4
    private def airLen(s: Seq[SearchResult[_]]) = s.flatMap { _ firstAirDate } |> longest max 5
    private def rjs(width: Int) = "%" + width + "s"
    private def ljs(width: Int) = "%-" + width + "s"

}
