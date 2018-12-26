package com.worthlesscog.tv.mede8er

import java.awt.{Dimension, Image}
import java.awt.image.BufferedImage
import java.io._
import java.nio.charset.{Charset, StandardCharsets}

import com.worthlesscog.tv.{asLeft, asRight, info, maybeIO, using, HttpOps, Maybe, Or, Pipe}
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
    def generate(s: TvSeries, dir: File): Maybe[String] =
        generate(s, TvSeries(), dir)

    private def generate(s1: TvSeries, s2: TvSeries, dir: File): Maybe[String] = {
        val name = s1.name or s2.name
        if (s1.seasons exists { _ exists { _.number contains 1 } })
            for {
                d <- createRoot(dir, name)
                _ <- viewXml("Movie") |> createXmlFile(d, "View")
                _ <- downloadImage(d, ABOUT, s1.backdropUrl or s2.backdropUrl, ABOUT_DIMENSIONS)
                _ <- downloadImage(d, FOLDER, s1.posterUrl or s2.posterUrl, MOVIE_DIMENSIONS)
                _ <- createSeriesXml(s1, s2) |> createXmlFile(d, name get)
                status <- createSeasons(d, s1, s2, s1.seasons)
            } yield status
        else
            for {
                d <- createRoot(dir, name)
                status <- createSeasons(d, s1, s2, s1.seasons)
            } yield status
    }

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
            // info(s"Creating $f\n")
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

    def fos(file: File) =
        new FileOutputStream(file)

    private def downloadImage(dir: File, stub: String, url: Option[String], dim: Dimension) =
        url match {
            case Some(u) =>
                val f = new File(dir, stub + ".jpg")
                // info(s"Creating $f\n")
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

    // XXX - for missing fields, examine supplied data to provide? i.e. number of seasons
    private def createSeriesXml(s1: TvSeries, s2: TvSeries) =
        titleXml(
            cast = Seq(
                s1.language or s2.language or UNKNOWN,
                (s1.numberOfSeasons or s2.numberOfSeasons).fold { "?" } { _ + " season(s)" },
                s1.status or s2.status or UNKNOWN
            ), // XXX - Abuse cast list
            date = s1.airDate or s2.airDate or "",
            director = "",
            genres = s1.genres or s2.genres or Nil,
            plot = s1.overview or s2.overview or "",
            rating = 10 * (s1.rating or s2.rating or 0.0) toInt,
            runtime = (s1.runtime or s2.runtime).fold { UNKNOWN } { _.toString },
            title = s1.name or s2.name get,
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

    // private def isPrintable(c: Char) =
    //     !Character.isISOControl(c) && Option(Character.UnicodeBlock.of(c)).fold(false)(_ ne Character.UnicodeBlock.SPECIALS)

    private def createSeasons(dir: File, s1: TvSeries, s2: TvSeries, seasons: Option[Seq[TvSeason]]) =
        seasons.fold[Maybe[String]] { DONE |> asRight } { forAll(_, createSeason(dir, s1, s2)) }

    private def forAll[T, U](things: Seq[T], f: T => Maybe[U]): Maybe[String] =
        if (things isEmpty)
            DONE |> asRight
        else f(things.head) match {
            case Left(error) => error |> asLeft
            case _           => forAll(things tail, f)
        }

    private def createSeason(dir: File, s1: TvSeries, s2: TvSeries)(s: TvSeason) =
        s.number match {
            case Some(n) =>
                val name = f"S$n%02d"
                val t = s2.seasons.flatMap { _ find { _.number contains n } } or TvSeason()
                for {
                    d <- createDir(dir)(name)
                    _ <- viewXml("Photo") |> createXmlFile(d, "View")
                    _ <- downloadImage(d, FOLDER, s.posterUrl or t.posterUrl, MOVIE_DIMENSIONS)
                    _ <- createSeasonXml(s, t, name, s1.runtime or s2.runtime, s1.genres or s2.genres) |> createXmlFile(d, name)
                    _ <- createEpisodes(d, s1, s2, n, t, s.episodes)
                } yield d

            case _ =>
                "Season number can't be empty\n" |> asLeft
        }

    private def createSeasonXml(s: TvSeason, t: TvSeason, title: String, runtime: Option[Int], genres: Option[Seq[String]]) = {
        val episodes = s.numberOfEpisodes or t.numberOfEpisodes or s.episodes.fold { 0 } { _.size }
        val rating = s.episodes.fold { 0.0 } { s => if (s isEmpty) 0.0 else (s flatMap { _.rating } sum) / s.size }

        titleXml(
            cast = Seq(s"$episodes episode(s)"), // XXX - Abuse cast list
            date = s.airDate or t.airDate or "",
            director = "",
            genres = genres or Nil,
            plot = s.overview or t.overview or "",
            rating = 10 * rating toInt,
            runtime = runtime.fold { UNKNOWN } { _.toString },
            title = title,
            year = "")
    }

    private def createEpisodes(dir: File, s1: TvSeries, s2: TvSeries, season: Int, t: TvSeason, episodes: Option[Seq[TvEpisode]]) =
        episodes.fold[Maybe[String]] { DONE |> asRight } { forAll(_, createEpisode(dir, s1, s2, season, t)) }

    private def createEpisode(dir: File, s1: TvSeries, s2: TvSeries, season: Int, t: TvSeason)(e: TvEpisode) =
        e.number match {
            case Some(n) =>
                val f = t.episodes.flatMap { _ find { _.number contains n } } or TvEpisode()
                val name = e.name or f.name or "Episode " + n
                val number = f"$n%02d"
                for {
                    d <- createDir(dir)(f"$number - $name")
                    _ <- downloadImage(d, FOLDER, e.screenshotUrl or f.screenshotUrl, PHOTO_DIMENSIONS)
                    _ <- createTitleXml(e, f, s1.runtime or s2.runtime, s1.genres or s2.genres) |> createXmlFile(d, f"S$season%02dE$number%s")
                } yield d

            case _ =>
                "Episode number can't be empty\n" |> asLeft
        }

    private def createTitleXml(e: TvEpisode, f: TvEpisode, runtime: Option[Int], genres: Option[Seq[String]]) =
        titleXml(
            cast = (e.cast or f.cast).fold[Seq[String]](Nil) { castNames },
            date = e.airDate or f.airDate or "",
            director = (e.crew or f.crew).fold { "" } { director },
            genres = genres or Nil,
            plot = e.overview or f.overview or "",
            rating = (e.rating or f.rating).fold(0) { 10 * _ toInt },
            runtime = runtime.fold { UNKNOWN } { _.toString },
            title = e.name or f.name or "",
            year = "")

    private def castNames(cast: Seq[Role]) =
        cast flatMap { _.name }

    private def director(crew: Seq[Role]) =
        crew find { _.role contains "Director" } flatMap { _.name } or ""

    def merge(series: TvSeries, series2: TvSeries, target: File): Maybe[String] =
        generate(series, series2, target)

    def resize(target: File): Maybe[String] =
        if (new File(target, target.getName + ".xml") exists)
            for {
                _ <- verifyImage(new File(target, ABOUT_JPG), ABOUT_DIMENSIONS)
                _ <- verifyImage(new File(target, FOLDER_JPG), MOVIE_DIMENSIONS)
                s <- forAll(target.listFiles filter { _.isDirectory }, resizeSeason)
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
                        temp <- maybeIO { createTempFile(f.getParentFile) }
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
                s <- forAll(target.listFiles filter { _.isDirectory }, resizeEpisode)
            } yield s
        else
            DONE |> asRight

    private def resizeEpisode(target: File) =
        if (target.getName matches """^\d\d - .*""")
            verifyImage(new File(target, FOLDER_JPG), PHOTO_DIMENSIONS)
        else
            DONE |> asRight

}
