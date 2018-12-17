package com.worthlesscog.tv.tvdb

import java.net.HttpURLConnection.HTTP_NOT_FOUND

import com.worthlesscog.tv.{asInt, asLeft, asRight, Maybe, Or, Pairs, Pipe}
import com.worthlesscog.tv.HttpOps.{getJson, httpCode, postJsonToJson}
import com.worthlesscog.tv.data._
import com.worthlesscog.tv.tvdb.Protocols._
import spray.json.{pimpAny, JsValue}

// fanart    graphical/text    Top about.jpg
// poster                      Top folder.jpg
// season    0/1/2/3/4...      Mid folder.jpg

// XXX - only fanart supports the graphical/text subKey, consequently other types are an uncategorized mixture
// XXX - the same item can have different ratings on site and in the API
object Tvdb extends TvDatabase {

    val API = "https://api.thetvdb.com/"
    val ART_URL = "https://www.thetvdb.com/banners/"

    val LOGIN = API + "login"
    val SEARCH = API + "search/series"
    val SERIES = API + "series/"

    val ACTORS = "/actors"
    val EPISODES = "/episodes"
    val EPISODES_QUERY = "/episodes/query"
    val EPISODES_SUMMARY = "/episodes/summary"
    val IMAGES_QUERY = "/images/query"

    val FANART = "fanart"
    val POSTER = "poster"
    val SEASON = "season"

    val NO_DATA = "No data found"
    val NO_ID = "ID missing"
    val UNKNOWN = "Unknown"

    val imageLanguages = Seq("en", "de", "fr")

    def databaseId = "tvdb"

    def databaseName = "The TVDB"

    def authenticate(c: Credentials) = {
        val a = c.token.fold(Authn()) { Authn }
        postJsonToJson(LOGIN, a.toJson) fold(
            asLeft,
            _ fold(
                httpCode(LOGIN),
                authenticationPass))
    }

    private def authenticationPass(v: JsValue) =
        v.convertTo[Authz].token |> TvdbToken |> asRight

    def search(name: String)(implicit t: Token, lang: String) =
        for {
            search <- maybe(convertSeriesSearch, SEARCH, Seq(("name", name)))
            results <- extractSearchResults(search)
        } yield results

    private def maybe[T](convert: JsValue => Maybe[T], url: String, parameters: Pairs = Nil)(implicit t: Token, lang: String) =
        getRawJson(url, parameters) fold(
            asLeft,
            _ fold(
                httpCode(url, parameters),
                convert))

    private def getRawJson(url: String, parameters: Pairs = Nil, headers: Pairs = Nil)(implicit t: Token, lang: String) =
        getJson(url, parameters, headers ++ auth)

    private def auth(implicit t: Token, lang: String) =
        Seq(("Authorization", "Bearer " + t.asInstanceOf[TvdbToken].sessionKey), ("Accept-Language", lang))

    private def convertSeriesSearch(j: JsValue) =
        j.convertTo[SeriesSearch] |> asRight

    private def extractSearchResults(s: SeriesSearch) =
        s.data match {
            case Some(results) =>
                results
                    .filter { r => r.id.nonEmpty && r.seriesName.nonEmpty }
                    .map { r => SearchResult(databaseId, r.id.get, r.seriesName.get, r.firstAired, None, None) } |> asRight

            case _ =>
                Nil |> asRight
        }

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]])(implicit token: Token, lang: String) =
        asInt(identifier) fold(_ => searchBySlug(identifier), asRight) match {
            case Left(error) =>
                error |> asLeft

            case Right(id) =>
                for {
                    series <- maybe(extractSeries, SERIES + id)
                    actors <- getActors(id)
                    requiredSeasons = seasonNumbers.getOrElse(1 to 99 toSet).toSeq.sorted
                    availableSeasons <- maybe(extractAvailableSeasons, SERIES + id + EPISODES_SUMMARY)
                    seasons = requiredSeasons filter { availableSeasons contains } map { _.toString }
                    episodes <- download(paginatedDownload, SERIES + id + EPISODES_QUERY, Nil, "airedSeason", seasons)
                    fanart <- downloadLanguageImages(SERIES + id + IMAGES_QUERY, keyType(FANART))
                    posters <- downloadLanguageImages(SERIES + id + IMAGES_QUERY, keyType(POSTER))
                    images <- download(downloadLanguageImages, SERIES + id + IMAGES_QUERY, keyType(SEASON), "subKey", seasons)
                    series <- buildTvSeries(series, actors, episodes, fanart ++ posters ++ images)
                } yield series
        }

    private def searchBySlug(slug: String)(implicit token: Token, lang: String) =
        for {
            search <- maybe(convertSeriesSearch, SEARCH, Seq(("slug", slug)))
            id <- extractSeriesId(search)
        } yield id

    private def extractSeriesId(s: SeriesSearch) =
        s.data match {
            case Some(results) =>
                results.size match {
                    case 1 => Either.cond(results.head.id nonEmpty, results.head.id.get, NO_ID)
                    case _ => "Too many matches\n" |> asLeft
                }

            case _ =>
                NO_DATA |> asLeft
        }

    private def extractSeries(v: JsValue) =
        v.convertTo[SeriesData].data match {
            case Some(series) => Either.cond(series.id nonEmpty, series, NO_ID)
            case _            => NO_DATA |> asLeft
        }

    private def getActors(id: Int)(implicit t: Token, lang: String) =
        getRawJson(SERIES + id + ACTORS) fold(
            asLeft, {
            case Left(HTTP_NOT_FOUND) => Nil |> asRight // 404 - there aren't any, that's OK
            case Left(code)           => httpCode(SERIES + id + ACTORS)(code)
            case Right(j)             => extractActors(j)
        })

    private def extractActors(v: JsValue) =
        v.convertTo[SeriesActors].data match {
            case Some(actors) => actors |> asRight
            case _            => NO_DATA |> asLeft
        }

    private def extractAvailableSeasons(v: JsValue) = {
        v.convertTo[SeriesEpisodesSummary].data.airedSeasons match {
            case Some(Nil)     => Set.empty[Int] |> asRight
            case Some(seasons) => seasons.map { _.toInt }.toSet |> asRight
            case _             => Set.empty[Int] |> asRight
        }
    }

    private def download[T](f: (String, Pairs) => Maybe[Seq[T]], url: String, parameters: Pairs, label: String, ps: Seq[String], tees: Seq[T] = Nil): Maybe[Seq[T]] =
        if (ps isEmpty)
            tees |> asRight
        else f(url, parameters :+ (label, ps.head)) match {
            case Left(error) => error |> asLeft
            case Right(seq)  => download(f, url, parameters, label, ps.tail, tees ++ seq)
        }

    private def keyType(t: String) =
        Seq(("keyType", t))

    private def paginatedDownload(url: String, parameters: Pairs = Nil)(implicit t: Token, lang: String) = {
        def load(episodes: Seq[Episode], page: Int): Maybe[Seq[Episode]] = {
            val p = parameters :+ ("page", page.toString)
            getRawJson(url, p) match {
                case Left(error) =>
                    error |> asLeft

                case Right(Left(code)) =>
                    httpCode(url, p)(code)

                case Right(Right(j)) =>
                    val e = j.convertTo[SeriesEpisodes]
                    val last = e.links.fold(page) { _.last or page }
                    e.data match {
                        case Some(es) =>
                            val f = es filter (_.airedSeason nonEmpty)
                            if (page != last)
                                load(episodes ++ f, page + 1)
                            else
                                (episodes ++ f) |> asRight

                        case _ =>
                            episodes |> asRight
                    }
            }
        }

        load(Nil, 1)
    }

    private def downloadLanguageImages(url: String, parameters: Pairs)(implicit t: Token, lang: String) = {
        def load(results: Seq[ImageResult], l: Seq[String]): Maybe[Seq[ImageResult]] =
            if (l isEmpty)
                results |> asRight
            else getRawJson(url, parameters)(t, l.head) match {
                case Left(error) =>
                    error |> asLeft

                case Right(Left(HTTP_NOT_FOUND)) =>
                    // 404 - there aren't any, that's OK
                    load(results, l.tail)

                case Right(Left(code)) =>
                    httpCode(url, parameters)(code)

                case Right(Right(j)) =>
                    val i = j.convertTo[ImageSearch]
                    i.data match {
                        case Some(images) =>
                            val ne = images filter (_.fileName nonEmpty)
                            load(results ++ ne, l.tail)

                        case _ =>
                            load(results, l.tail)
                    }
            }

        // XXX - 'Accept-Language: *' is unsupported so pull 3 (probably) most popular
        load(Nil, imageLanguages)
    }

    private def buildTvSeries(s: Series, actors: Seq[Actor], episodes: Seq[Episode], images: Seq[ImageResult]) =
        TvSeries(
            airDate = s.firstAired,
            backdropUrl = highestRated(images, FANART),
            genres = s.genre,
            language = None,
            name = s.seriesName,
            numberOfSeasons = None,
            overview = s.overview,
            posterUrl = highestRated(images, POSTER),
            rating = s.siteRating,
            runtime = s.runtime map { _.toInt },
            seasons = Some(buildTvSeasons(s, episodes, actors, images)),
            votes = s.siteRatingCount
        ) |> asRight

    // XXX - fix this, it's awful
    private def highestRated(images: Seq[ImageResult], keyType: String, resolution: Option[String] = None, subKey: Option[String] = None) = {
        val f1 = resolution match {
            case Some(r) => images.filter { _.resolution contains r }
            case _       => images
        }
        val f2 = subKey match {
            case Some(k) => f1 filter { _.subKey contains k }
            case _       => f1
        }
        f2
            .filter { _.keyType contains keyType }
            .sortBy { _.ratingsInfo.fold(0.0) { _.average or 0.0 } }
            .reverse
            .headOption
            .flatMap { i => artUrl(i.fileName) }
    }

    // XXX - kludge to cope with "" instead of None; presumably there's some Spray magic that could be done?
    private def artUrl(o: Option[String]): Option[String] =
        o match {
            case None     => None
            case Some("") => None
            case Some(s)  => Some(ART_URL + s)
        }

    private def buildTvSeasons(s: Series, episodes: Seq[Episode], actors: Seq[Actor], images: Seq[ImageResult]) =
        episodes.flatMap { _.airedSeason }.distinct.sorted map buildTvSeason(s, episodes, actors, images)

    private def buildTvSeason(s: Series, episodes: Seq[Episode], actors: Seq[Actor], images: Seq[ImageResult])(number: Int) =
        TvSeason(
            airDate = None,
            episodes = Some(episodes filter { _.airedSeason contains number } sortBy { _.airedEpisodeNumber } map buildTvEpisode(s, actors)),
            number = Some(number),
            numberOfEpisodes = None,
            overview = None,
            posterUrl = highestRated(images, SEASON, None, Some(number toString))
        )

    private def buildTvEpisode(s: Series, actors: Seq[Actor])(e: Episode) =
        TvEpisode(
            airDate = e.firstAired,
            cast = Some(actors sortBy { _.sortOrder } map toRole),
            crew = None,
            name = e.episodeName,
            number = e.airedEpisodeNumber,
            overview = e.overview,
            screenshotUrl = artUrl(e.filename),
            rating = e.siteRating,
            votes = e.siteRatingCount
        )

    private def toRole(a: Actor) =
        Role(
            name = a.name,
            role = a.role
        )

}
