package com.worthlesscog.tv.mede8er

import java.awt.{Dimension, Image}
import java.awt.image.BufferedImage
import java.io._
import java.nio.charset.{Charset, StandardCharsets}

import com.worthlesscog.tv.{asLeft, asRight, first, info, maybeIO, using, HttpOps, Maybe, Or, Pipe}
import com.worthlesscog.tv.data._
import javax.imageio.{IIOImage, ImageIO, ImageWriteParam, ImageWriter}

import scala.xml.{Elem, PrettyPrinter, XML}

// XXX - look for option folds
// XXX - check tail calls are in fact tail calls

// XXX - still possibly needed for sanitize
//       long text
//       generic text
//       (ACTOR NAME)
//       (SPECIAL GUEST STAR..)
//       "... word.New sentence" - what about "A.C.R.O.N.Y.M.S." ?
//       Non-unicode
//       embedded HTML tags for italics etc.?

class Mede8er extends MediaPlayer {

    this: HttpOps =>

    val DONE = "Done\n"
    val UNKNOWN = "??"

    val ABOUT = "about"
    val ABOUT_JPG = ABOUT + ".jpg"
    val FOLDER = "folder"
    val FOLDER_JPG = FOLDER + ".jpg"

    val ABOUT_DIMENSIONS = new Dimension(1280, 720)
    val MOVIE_DIMENSIONS = new Dimension(119, 173)
    val PHOTO_DIMENSIONS = new Dimension(237, 179)

    // about.jpg    "Info" button picture for folders
    // cover.jpg    ??
    // fanart.jpg   ??
    // folder.jpg   Folder image when browsing above

    def playerId = "mede8er"

    def playerName = "Mede8er"

    // XXX - allow distortion of posters? they're very small, it's probably better that they fill the space
    def merge(series: Seq[TvSeries], dir: File): Maybe[String] = {
        val name = firstName(series)
        val seasons = series flatMap (_ seasons)
        val seasonNumbers = numbers(seasons)
        if (seasonNumbers contains 1)
            for {
                d <- createRoot(dir, name)
                _ <- viewXml("Movie") |> createXmlFile(d, "View")
                _ <- downloadImage(d, ABOUT, firstBackdropUrl(series), ABOUT_DIMENSIONS)
                _ <- downloadImage(d, FOLDER, firstPosterUrl(series), MOVIE_DIMENSIONS)
                _ <- createSeriesXml(series) |> createXmlFile(d, name get)
                status <- createSeasons(d, series, seasons, seasonNumbers)
            } yield status
        else
            for {
                d <- createRoot(dir, name)
                status <- createSeasons(d, series, seasons, seasonNumbers)
            } yield status
    }

    private def firstName[T <: Name](s: Seq[T]) =
        first(s) { _ name }

    def numbers[T <: Number](s: Seq[Seq[T]]) =
        s.flatten.flatMap(_ number).distinct.sorted

    private def createRoot(dir: File, name: Option[String]) =
        name.fold[Maybe[File]] { "Series name can not be empty\n" |> asLeft } { createDir(dir) }

    private def createDir(dir: File)(name: String) = {
        val s = sanitizeFilename(name)
        val f = new File(dir, s)
        info(s"Creating $f\n")
        f.mkdirs
        if (f.exists && f.isDirectory)
            f |> asRight
        else
            s"Can't create directory $f\n" |> asLeft
    }

    private def sanitizeFilename(s: String) =
        s replace('/', '-') replace('\\', '-') trim // replace (':', '-')

    private def viewXml(style: String) =
        <FolderTag>
            <ViewMode>{style}</ViewMode>
        </FolderTag>

    private def createXmlFile(dir: File, name: String)(e: Elem) = {
        def formatAndCreate(f: File) = {
            using(f |> fos |> osw(StandardCharsets.UTF_8)) {
                val fmt = new PrettyPrinter(2048, 2).format(e) |> XML.loadString
                XML.write(_, fmt, "UTF-8", true, null)
            }
            f |> asRight
        }
        def osw(charSet: Charset)(stream: OutputStream) = new OutputStreamWriter(stream, charSet)

        val s = sanitizeFilename(name)
        val f = new File(dir, s + ".xml")
        maybeIO { formatAndCreate(f) }
    }

    private def fos(file: File) =
        new FileOutputStream(file)

    private def downloadImage(dir: File, stub: String, url: Option[String], dim: Dimension) =
        url match {
            case Some(u) =>
                val f = new File(dir, stub + ".jpg")
                for {
                    i <- readImage(u)
                    r <- resizeImage(i, dim, 2)
                    f <- saveJpg(f, r)
                } yield f

            case _ =>
                dir |> asRight
        }

    private def readImage(url: String) = {
        def convertImage(bytes: Array[Byte]) = new ByteArrayInputStream(bytes) |> ImageIO.read |> asRight

        getBytes(url) fold(
            asLeft,
            _ fold(
                httpCode(url),
                bs => maybeIO { convertImage(bs) } fold(
                    _ + " - " + url + "\n" |> asLeft,
                    asRight)))
    }

    private def resizeImage(i: BufferedImage, dim: Dimension, maxSteps: Int = 5) =
        if (i.getWidth > dim.width || i.getHeight > dim.height) {
            val (w, h) = (i.getWidth toFloat, i.getHeight toFloat)
            val r = (w / dim.width) max (h / dim.height)
            val (tw, th) = ((w / r) round, (h / r) round)
            i |> scaleImage(tw, th, maxSteps) |> asRight
        } else {
            i |> asRight
        }

    private def scaleImage(targetWidth: Int, targetHeight: Int, maxSteps: Int)(i: BufferedImage): BufferedImage = {
        def draw(image: BufferedImage, width: Int, height: Int): BufferedImage = {
            val b = new BufferedImage(width, height, image.getType)
            val g = b.createGraphics
            val i = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
            g.drawImage(i, 0, 0, null)
            g.dispose()
            b
        }

        val (w, h) = (i.getWidth / 2, i.getHeight / 2)
        if (w < targetWidth || h < targetHeight || maxSteps <= 1)
            draw(i, targetWidth, targetHeight)
        else
            scaleImage(targetWidth, targetHeight, maxSteps - 1)(draw(i, w, h))
    }

    // XXX - does this work for .something_else to .jpg?
    private def saveJpg(file: File, i: BufferedImage) = {
        def write(w: ImageWriter) =
            using(file |> fos |> ImageIO.createImageOutputStream) { s =>
                w.setOutput(s)
                w.write(null, new IIOImage(i, null, null), compressionParams(w, 1.0f))
                w.dispose()
                file |> asRight
            }

        def compressionParams(writer: ImageWriter, quality: Float) = {
            val p = writer.getDefaultWriteParam
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
            p.setCompressionQuality(quality)
            p
        }

        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (writers hasNext)
            maybeIO { write(writers.next) }
        else
            "No jpg writer available\n" |> asLeft
    }

    private def firstBackdropUrl(s: Seq[TvSeries]) =
        first(s) { _ backdropUrl }

    private def firstPosterUrl[T <: PosterUrl](s: Seq[T]) =
        first(s) { _ posterUrl }

    // XXX - for missing fields, examine supplied data to provide? i.e. number of seasons
    private def createSeriesXml(series: Seq[TvSeries]) =
        titleXml(
            cast = Seq(
                firstLanguage(series) or UNKNOWN,
                firstNumberOfSeasons(series).fold { UNKNOWN } { _ + " season(s)" },
                firstRated(series) or UNKNOWN,
                firstStatus(series) or UNKNOWN
            ), // XXX - Abuse cast list
            date = firstAirDate(series) or "",
            director = "",
            genres = firstGenres(series) or Nil,
            plot = firstOverview(series) or "",
            rating = averageRating(series),
            runtime = firstRuntime(series).fold { UNKNOWN } { _.toString },
            title = firstName(series) get,
            year = "")

    private def titleXml(title: String, year: String, rating: Int, plot: String, date: String, runtime: String, genres: Seq[String], director: String, cast: Seq[String]) =
        <details>
            <movie>
                <title>{title}</title>
                <year>{year}</year>
                <rating>{rating}</rating>
                <plot>{plot}</plot>
                <date>{date}</date>
                <runtime>{runtime}</runtime>
                <genres>{genres map genre}</genres>
                <director>
                    <name>{director}</name>
                </director>
                <cast>{cast map actor}</cast>
            </movie>
        </details>

    private def genre(s: String) =
        <genre>{s}</genre>

    private def actor(s: String) =
        <actor>{s}</actor>

    private def firstLanguage(s: Seq[TvSeries]) =
        first(s) { _ language }

    private def firstNumberOfSeasons(s: Seq[TvSeries]) =
        first(s) { _ numberOfSeasons }

    private def firstRated(s: Seq[TvSeries]) =
        first(s) { _ rated }

    private def firstStatus(s: Seq[TvSeries]) =
        first(s) { _ status }

    private def firstAirDate[T <: AirDate](s: Seq[T]) =
        first(s) { _ airDate }

    private def firstGenres(s: Seq[TvSeries]) =
        first(s) { _ genres }

    private def firstOverview[T <: Overview](s: Seq[T]) =
        first(s) { _ overview }

    def averageRating[T <: Rating](s: Seq[T]) =
        s.foldLeft(0.0, 0) {
            case ((total, votes), t) => (t.rating, t.votes) match {
                case (Some(r), Some(v)) => (total + r * v, votes + v)
                case (Some(r), None)    => (total + r, votes + 1)
                case _                  => (total, votes)
            }
        } match {
            case (_, 0) => 0
            case (r, v) => 10 * (r / v) toInt
        }

    private def firstRuntime(s: Seq[TvSeries]) =
        first(s) { _ runtime }

    // private def isPrintable(c: Char) =
    //     !Character.isISOControl(c) && Option(Character.UnicodeBlock.of(c)).fold(false)(_ ne Character.UnicodeBlock.SPECIALS)

    private def createSeasons(dir: File, series: Seq[TvSeries], seasons: Seq[Seq[TvSeason]], numbers: Seq[Int]) =
        forAll(numbers, createSeason(dir, series, seasons))

    private def forAll[T, U](things: Seq[T], f: T => Maybe[U]): Maybe[String] =
        if (things isEmpty)
            DONE |> asRight
        else f(things.head) match {
            case Left(error) => error |> asLeft
            case _           => forAll(things tail, f)
        }

    private def createSeason(dir: File, series: Seq[TvSeries], seasons: Seq[Seq[TvSeason]])(n: Int) = {
        val ss = numbered(seasons, n)
        val name = f"S$n%02d"
        for {
            d <- createDir(dir)(name)
            _ <- viewXml("Photo") |> createXmlFile(d, "View")
            _ <- downloadImage(d, FOLDER, firstPosterUrl(ss), MOVIE_DIMENSIONS)
            episodes = ss flatMap { _ episodes }
            episodeNumbers = numbers(episodes)
            _ <- createSeasonXml(series, ss, episodes, name, episodeNumbers) |> createXmlFile(d, name)
            _ <- createEpisodes(d, series, n, episodes, episodeNumbers)
        } yield d
    }

    def numbered[T <: Number](s: Seq[Seq[T]], n: Int) =
        s flatMap { _ filter { _.number contains n } }

    private def createSeasonXml(series: Seq[TvSeries], seasons: Seq[TvSeason], episodes: Seq[Seq[TvEpisode]], title: String, numbers: Seq[Int]) = {
        val numberOfEpisodes = firstNumberOfEpisodes(seasons) or numbers.max

        titleXml(
            cast = Seq(s"$numberOfEpisodes episode(s)"), // XXX - Abuse cast list
            date = firstAirDate(seasons) or "",
            director = "",
            genres = firstGenres(series) or Nil,
            plot = firstOverview(seasons) or "",
            rating = averageRating(episodes flatten),
            runtime = firstRuntime(series).fold { UNKNOWN } { _ toString },
            title = title,
            year = "")
    }

    private def firstNumberOfEpisodes(s: Seq[TvSeason]) =
        first(s) { _ numberOfEpisodes }

    private def createEpisodes(dir: File, series: Seq[TvSeries], season: Int, episodes: Seq[Seq[TvEpisode]], numbers: Seq[Int]) =
        forAll(numbers, createEpisode(dir, series, season, episodes))

    private def createEpisode(dir: File, series: Seq[TvSeries], season: Int, episodes: Seq[Seq[TvEpisode]])(n: Int) = {
        val es = numbered(episodes, n)
        val name = firstName(es) or "Episode " + n
        val number = f"$n%02d"
        for {
            d <- createDir(dir)(f"$number - $name")
            _ <- downloadImage(d, FOLDER, firstScreenshotUrl(es), PHOTO_DIMENSIONS)
            _ <- createTitleXml(es, firstRuntime(series), firstGenres(series)) |> createXmlFile(d, f"S$season%02dE$number%s")
        } yield d
    }

    private def firstScreenshotUrl(s: Seq[TvEpisode]) =
        first(s) { _ screenshotUrl }

    private def createTitleXml(episodes: Seq[TvEpisode], runtime: Option[Int], genres: Option[Seq[String]]) =
        titleXml(
            cast = firstCast(episodes).fold[Seq[String]](Nil) { castNames },
            date = firstAirDate(episodes) or "",
            director = firstCrew(episodes).fold { "" } { director },
            genres = genres or Nil,
            plot = firstOverview(episodes) or "",
            rating = averageRating(episodes),
            runtime = runtime.fold { UNKNOWN } { _ toString },
            title = firstName(episodes) or "",
            year = "")

    private def firstCast(s: Seq[TvEpisode]) =
        first(s) { _ cast }

    private def castNames(cast: Seq[Role]) =
        cast flatMap { _ name } distinct

    private def firstCrew(s: Seq[TvEpisode]) =
        first(s) { _ crew }

    private def director(crew: Seq[Role]) =
        crew find { _.role contains "Director" } flatMap { _ name } or ""

    def resize(target: File): Maybe[String] =
        if (new File(target, target.getName + ".xml") exists)
            for {
                _ <- verifyImage(new File(target, ABOUT_JPG), ABOUT_DIMENSIONS)
                _ <- verifyImage(new File(target, FOLDER_JPG), MOVIE_DIMENSIONS)
                s <- forAll(target.listFiles filter { _ isDirectory }, resizeSeason)
            } yield s
        else
            target + " doesn't look like a TV series root\n" |> asLeft

    private def verifyImage(f: File, dim: Dimension): Maybe[String] =
        maybeIO { readImage(f) } match {
            case Left(error) =>
                error |> asLeft

            case Right(i) =>
                if (i.getWidth > dim.width || i.getHeight > dim.height)
                    for {
                        newI <- resizeImage(i, dim)
                        temp <- maybeIO { createTempFile(f getParentFile) }
                        _ <- saveJpg(temp, newI)
                        _ <- maybeIO { deleteFile(f) }
                        s <- maybeIO { rename(temp, f) }
                    } yield s
                else
                    DONE |> asRight
        }

    private def readImage(image: File) =
        ImageIO.read(image) |> asRight

    private def createTempFile(target: File) =
        File.createTempFile("", "", target) |> asRight

    private def deleteFile(target: File) =
        if (target delete)
            target |> asRight
        else
            "Delete failed for $target\n" |> asLeft

    private def rename(source: File, target: File) =
        if (source renameTo target)
            DONE |> asRight
        else
            "Can't rename $source to $target\n" |> asLeft

    private def resizeSeason(target: File) =
        if (target.getName matches """^S\d\d$""")
            for {
                _ <- verifyImage(new File(target, FOLDER_JPG), MOVIE_DIMENSIONS)
                s <- forAll(target.listFiles filter { _ isDirectory }, resizeEpisode)
            } yield s
        else
            DONE |> asRight

    private def resizeEpisode(target: File) =
        if (target.getName matches """^\d\d - .*""")
            verifyImage(new File(target, FOLDER_JPG), PHOTO_DIMENSIONS)
        else
            DONE |> asRight

}
