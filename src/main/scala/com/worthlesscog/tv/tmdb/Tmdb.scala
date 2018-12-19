package com.worthlesscog.tv.tmdb

import com.worthlesscog.tv.{asInt, asLeft, asRight, jsInt, Approx, HttpOps, Maybe, Or, Pipe}
import com.worthlesscog.tv.data.{Credentials, Role, Token, TvDatabase, TvEpisode, TvSeason, TvSeries, SearchResult => ApiSearchResult}
import com.worthlesscog.tv.tmdb.Protocols._
import spray.json.JsValue

class Tmdb extends TvDatabase {

    this: HttpOps =>

    val API = "https://api.themoviedb.org/3/"

    val CONFIGURATION = API + "configuration"
    val GENRES = API + "genre/movie/list"
    val LANGUAGES = "/languages"
    val TV_DETAILS = API + "tv/"
    val TV_GENRES = API + "genre/tv/list"
    val TV_SEARCH = API + "search/tv"

    val TV_CREDITS = "/credits"
    val TV_IMAGES = "/images"
    val TV_SEASON = "/season/"

    def databaseId = "tmdb"

    def databaseName = "The Movie Database"

    def authenticate(c: Credentials) =
        TmdbToken(c.token or "") |> asRight

    def search(name: String)(implicit t: Token, lang: String) =
        for {
            // XXX - tmdb contains some miscategorized shows, use both sets of genres
            movieGenres <- maybe(extractGenres)(GENRES, auth)
            tvGenres <- maybe(extractGenres)(TV_GENRES, auth)
            results <- pages(seqResult, continue)(TV_SEARCH, auth ++ Seq(("query", name)))
            searchResults <- convertResults(results, movieGenres ++ tvGenres)
        } yield searchResults

    private def auth(implicit t: Token, lang: String) =
        Seq(("api_key", t.asInstanceOf[TmdbToken].apiKey), ("language", lang))

    private def extractGenres(v: JsValue) =
        v.convertTo[Genres].genres
            .getOrElse(Nil)
            .filter(_.id nonEmpty)
            .map(g => g.id.get -> g.name.get)
            .toMap |> asRight

    private def continue(n: Int, j: JsValue) =
        jsInt(j, "page", 1) != jsInt(j, "total_pages", 1)

    private def seqResult(v: JsValue) =
        v.convertTo[SearchResult].results or Nil

    private def convertResults(results: Seq[Result], genres: Map[Int, String]) =
        results
            .filter { r => r.id.nonEmpty && r.name.nonEmpty }
            .map {
                r => ApiSearchResult(databaseId, r.id.get, r.name.get, r.first_air_date, r.original_language, r.genre_ids map { _ map genres })
            } |> asRight

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]])(implicit token: Token, lang: String) =
        for {
            showId <- asInt(identifier)
            config <- maybe(extractConfiguration)(CONFIGURATION, auth)
            languages <- maybe(extractLanguages)(CONFIGURATION + LANGUAGES, auth)
            show <- maybe(extractShow)(TV_DETAILS + showId, auth)
            credits <- maybe(extractCredits)(TV_DETAILS + showId + TV_CREDITS, auth)
            // XXX - waste unless you're downloading season #1
            images <- maybe(extractShowImages)(TV_DETAILS + showId + TV_IMAGES, auth(token, lang + ",null"))
            requiredSeasons = seasonNumbers.getOrElse(1 to 99 toSet).toSeq.sorted
            availableSeasons = show.seasons.fold { 1 to 99 toSet } { _ flatMap { _.season_number } toSet }
            seq = requiredSeasons filter { availableSeasons contains }
            seasons <- download(extractSeason, showId, seq)
            seasonCredits <- download(extractCredits, showId, seq, TV_CREDITS)
            seasonImages <- download(extractSeasonImages, showId, seq, TV_IMAGES)(token, lang + ",null")
            series <- buildTvSeries(show, languages, credits, images, seasons, seasonCredits, seasonImages)(config)
        } yield series

    private def extractConfiguration(v: JsValue) =
        v.convertTo[Configuration] |> asRight

    private def extractShow(v: JsValue) =
        v.convertTo[Show] |> asRight

    private def extractLanguages(v: JsValue) =
        v.convertTo[Languages] |> asRight

    private def extractCredits(v: JsValue) =
        v.convertTo[Credits] |> asRight

    private def extractShowImages(v: JsValue) =
        v.convertTo[ShowImages] |> asRight

    private def download[T](f: JsValue => Maybe[T], showId: Int, seasons: Seq[Int], tail: String = "")(implicit token: Token, lang: String) = {
        def load(tees: Seq[T], s: Seq[Int]): Maybe[Seq[T]] = {
            if (s isEmpty)
                tees |> asRight
            else maybe(f)(TV_DETAILS + showId + TV_SEASON + s.head + tail, auth) match {
                case Left(error) => error |> asLeft
                case Right(t)    => load(tees :+ t, s.tail)
            }
        }

        load(Nil, seasons)
    }

    private def extractSeason(v: JsValue) =
        v.convertTo[Season] |> asRight

    private def extractSeasonImages(v: JsValue) =
        v.convertTo[SeasonImages] |> asRight

    private def buildTvSeries(show: Show, languages: Languages, credits: Credits, images: ShowImages, seasons: Seq[Season], seasonCredits: Seq[Credits], seasonImages: Seq[SeasonImages])(implicit config: Configuration) =
        TvSeries(
            airDate = show.first_air_date,
            backdropUrl = highestRated(images.backdrops, 16.0 / 9.0) |> backdropUrl,
            genres = show.genres map flatGenres,
            language = show.original_language flatMap { lang => languages.items find { _.iso_639_1 contains lang } flatMap { _.english_name } },
            name = show.name,
            numberOfSeasons = show.number_of_seasons,
            overview = show.overview,
            posterUrl = highestRated(images.posters, 2.0 / 3.0) |> posterUrl,
            rating = show.vote_average,
            runtime = show.episode_run_time map { _.min },
            seasons = Some(buildTvSeasons(credits, seasons, show.seasons or Nil, seasonCredits, seasonImages)),
            votes = show.vote_count) |> asRight

    // 1 highest rated, correct aspect and no language
    // 2 highest rated, correct aspect and correct language
    // 3 highest rated, no language
    // 4 highest rated, correct language
    // 5 unlucky
    private def highestRated(images: Option[Seq[Image]], aspectRatio: Double) = {
        // i filter { _.aspect_ratio ~= aspectRatio } maxBy { _.vote_average }
        def ratio(s: Seq[Image]) = s filter { _.aspect_ratio ~= aspectRatio }
        def rated(s: Seq[Image]) = (s sortBy { _.vote_average } reverse).headOption toRight false

        // o.O
        images flatMap { i =>
            val (none, some) = i partition { _.iso_639_1 isEmpty }
            (none |> ratio |> rated) fold(
                _ => some |> ratio |> rated fold(
                    _ => none |> rated fold(
                        _ => some |> rated fold(
                            _ => None,
                            Some(_)),
                        Some(_)),
                    Some(_)),
                Some(_))
        }
    }

    private def flatGenres(genres: Seq[Genre]) =
        genres flatMap { _.name map { s => s split " & " } } flatten

    private def backdropUrl(image: Option[Image])(implicit config: Configuration) =
        image flatMap {
            _.file_path flatMap { p =>
                config.images flatMap { i =>
                    i.base_url flatMap { b =>
                        // XXX - either have to parse these ignoring "original" or
                        //       simply take the longest, which is ... "original"
                        //       assuming that is going to be the largest
                        i.backdrop_sizes map { s =>
                            b + longest(s) + p
                        }
                    }
                }
            }
        }

    private def longest(seq: Seq[String]) =
        seq maxBy { _.length }

    private def posterUrl(image: Option[Image])(implicit config: Configuration) =
        image flatMap {
            _.file_path flatMap { p =>
                config.images flatMap { i =>
                    i.base_url flatMap { b =>
                        i.poster_sizes map { s =>
                            b + longest(s) + p
                        }
                    }
                }
            }
        }

    private def buildTvSeasons(showCredits: Credits, seasons: Seq[Season], synopsis: Seq[SeasonSynopsis], credits: Seq[Credits], images: Seq[SeasonImages])(implicit config: Configuration) = {
        seasons map { s => buildTvSeason(showCredits, s, synopsis.find { _.id == s.id }, credits.find { _.id == s.id }, images.find { _.id == s.id }) }
    }

    private def buildTvSeason(showCredits: Credits, season: Season, synopsis: Option[SeasonSynopsis], credits: Option[Credits], images: Option[SeasonImages])(implicit config: Configuration) = {
        val combinedCast = (showCredits.cast ++ credits.flatMap { _.cast }) reduceOption { _ ++ _ }
        val combinedCrew = (showCredits.crew ++ credits.flatMap { _.crew }) reduceOption { _ ++ _ }

        TvSeason(
            airDate = season.air_date,
            episodes = season.episodes map buildTvEpisodes(combinedCast, combinedCrew),
            number = season.season_number,
            numberOfEpisodes = synopsis flatMap { _.episode_count },
            overview = season.overview,
            posterUrl = images flatMap { s => highestRated(s.posters, 2.0 / 3.0) |> posterUrl })
    }

    private def buildTvEpisodes(cast: Option[Seq[Cast]], crew: Option[Seq[Crew]])(episodes: Seq[Episode])(implicit config: Configuration) =
        episodes map buildTvEpisode(cast, crew)

    // XXX - download all screenshots for each episode and take highest rated?
    private def buildTvEpisode(cast: Option[Seq[Cast]], crew: Option[Seq[Crew]])(episode: Episode)(implicit config: Configuration) = {
        val allCast = (cast ++ episode.guest_stars) reduceOption { _ ++ _ }
        val allCrew = (crew ++ episode.crew) reduceOption { _ ++ _ }

        TvEpisode(
            airDate = episode.air_date,
            cast = allCast map castToRole,
            crew = allCrew map crewToRole,
            name = episode.name,
            number = episode.episode_number,
            overview = episode.overview,
            screenshotUrl = stillUrl(episode.still_path),
            rating = episode.vote_average,
            votes = episode.vote_count)
    }

    private def castToRole(cast: Seq[Cast]) =
        cast.distinct sortBy { _.order } map { c => Role(c.name, c.character) }

    private def crewToRole(crew: Seq[Crew]) =
        crew.distinct map { c => Role(c.name, c.job) }

    private def stillUrl(image: Option[String])(implicit config: Configuration) =
        image flatMap { p =>
            config.images flatMap { i =>
                i.base_url flatMap { b =>
                    i.still_sizes map { s =>
                        b + longest(s) + p
                    }
                }
            }
        }

}
