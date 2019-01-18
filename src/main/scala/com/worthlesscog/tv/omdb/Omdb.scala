package com.worthlesscog.tv.omdb

import com.worthlesscog.tv.{asLeft, asRight, jsField, jsStringToInt, HttpOps, Maybe, Or, Pairs, Pipe}
import com.worthlesscog.tv.data.{Credentials, Role, Token, TvDatabase, TvEpisode, TvSeason, TvSeries, SearchResult => ApiSearchResult}
import com.worthlesscog.tv.omdb.Protocols._
import spray.json.JsValue

class Omdb extends TvDatabase {

    this: HttpOps =>

    val API = "http://www.omdbapi.com/"

    def databaseId = "omdb"

    def databaseName = "The Open Movie Database"

    def authenticate(c: Credentials) =
        OmdbToken(c.token or "") |> asRight

    def search(name: String, t: Token, lang: String) =
        searchWithImplicits(name)(t)

    // XXX - Fix this to cope with failure response
    private def searchWithImplicits(name: String)(implicit t: Token) =
        for {
            results <- pages(searchResult, continue)(API, auth :+ search(name) :+ `type`("series") :+ jsonResults)
            searchResults <- convertResults(results)
        } yield searchResults

    private def searchResult(v: JsValue) =
        v.convertTo[SearchResult].Search or Nil

    private def continue(n: Int, j: JsValue) =
        n != jsStringToInt(j, "totalResults", n)

    private def auth(implicit t: Token) =
        Seq(("apikey", t.asInstanceOf[OmdbToken].apiKey))

    private def search(name: String) =
        ("s", name)

    private def `type`(t: String) =
        ("type", t)

    private def jsonResults =
        ("r", "json")

    private def convertResults(results: Seq[TitleSynopsis]) =
        results
            .filter { r => r.imdbID.nonEmpty && r.Title.nonEmpty }
            .map {
                r => ApiSearchResult(databaseId, r.imdbID.get, r.Title.get, r.Year, None, None)
            } |> asRight

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]], token: Token, lang: String) =
        getTvSeriesWithImplicits(identifier, seasonNumbers)(token)

    private def getTvSeriesWithImplicits(identifier: String, seasonNumbers: Option[Set[Int]])(implicit token: Token) =
        for {
            title <- maybe(ifSuccessful(extractTitle))(API, auth :+ id(identifier) :+ fullPlot :+ jsonResults)
            requiredSeasons = seasonNumbers.getOrElse(1 to 99 toSet).toSeq.sorted
            availableSeasons = title.totalSeasons.getOrElse("0").toInt
            // XXX - no sign of specials, usually masquerading as season 0
            sNos = requiredSeasons filter { n => n > 0 && n <= availableSeasons } map { _ toString }
            seasons <- download(ifSuccessful(extractSeason), auth :+ id(identifier) :+ jsonResults, "Season", sNos)
            eNos = seasons flatMap { _.Episodes flatMap { _ imdbID } }
            episodes <- download(ifSuccessful(extractEpisode), auth :+ `type`("episode") :+ fullPlot :+ jsonResults, "i", eNos)
            series <- buildTvSeries(title, seasons, episodes)
        } yield series

    // o.O - {"Response":"False","Error":"Movie not found!"}
    private def ifSuccessful[T](f: JsValue => T)(v: JsValue): Maybe[T] = {
        def error = jsField(v, "Error").fold("Unknown Error") { _.toString } |> asLeft

        jsField(v, "Response").fold[Maybe[T]](error) { x =>
            x.convertTo[String] match {
                case "True" => f(v) |> asRight
                case _      => error
            }
        }
    }

    private def extractTitle(v: JsValue) =
        v.convertTo[Title]

    private def id(identifier: String) =
        ("i", identifier)

    private def fullPlot =
        ("plot", "full")

    // XXX - tvdb does this pretty much
    private def download[T](f: JsValue => Maybe[T], parameters: Pairs, label: String, things: Seq[String], tees: Seq[T] = Nil): Maybe[Seq[T]] =
        if (things isEmpty)
            tees |> asRight
        else maybe(f)(API, parameters :+ (label, things.head)) match {
            case Left(error) => error |> asLeft
            case Right(t)    => download(f, parameters, label, things.tail, tees :+ t)
        }

    private def extractSeason(v: JsValue) =
        v.convertTo[Season]

    private def extractEpisode(v: JsValue) =
        v.convertTo[Episode]

    private def buildTvSeries(t: Title, seasons: Seq[Season], episodes: Seq[Episode]) =
        TvSeries(
            airDate = t Released, // XXX - format?
            backdropUrl = None,
            genres = t.Genre map splitCommaDelimited,
            language = t Language,
            name = t Title,
            numberOfSeasons = t.totalSeasons map { _ toInt },
            overview = t Plot,
            posterUrl = None,
            rating = t.imdbRating map { _ toDouble },
            runtime = None, // XXX - work out what's contained
            seasons = Some(buildTvSeasons(t, seasons, episodes)),
            status = None,
            votes = t.imdbVotes map unspecifiedInt
        ) |> asRight

    private def splitCommaDelimited(s: String) =
        s split "," map { _ trim }

    private def unspecifiedInt(s: String) =
        s.filter(_ isDigit).mkString toInt

    private def buildTvSeasons(s: Title, seasons: Seq[Season], episodes: Seq[Episode]) =
        seasons.flatMap(_.Season).sorted map { n => buildTvSeason(s, n, episodes filter { _.Season contains n }) }

    private def buildTvSeason(s: Title, number: String, episodes: Seq[Episode]) =
        TvSeason(
            airDate = None,
            episodes = Some(episodes sortBy { _.Episode } map buildTvEpisode(s)),
            number = Some(number toInt),
            numberOfEpisodes = Some(episodes size),
            overview = None,
            posterUrl = None
        )

    private def buildTvEpisode(s: Title)(e: Episode) =
        TvEpisode(
            airDate = e.Released,
            cast = e.Actors map { splitCommaDelimited(_) map { n => Role(Some(n), None) } },
            crew = None, // XXX - we *might* have the Director
            name = e.Title,
            number = e.Episode map { _ toInt },
            overview = e.Plot, // XXX - sanitize
            screenshotUrl = None,
            rating = e.imdbRating map { _ toDouble },
            votes = e.imdbVotes map unspecifiedInt
        )

}
