package com.worthlesscog.tv.maze

import com.worthlesscog.tv._
import com.worthlesscog.tv.TextUtils._
import com.worthlesscog.tv.data.{Credentials, Token, TvDatabase, TvEpisode, TvSeason, TvSeries, Role => ApiRole, SearchResult => ApiSearchResult}
import com.worthlesscog.tv.maze.Protocols._
import spray.json.JsValue

class Maze extends TvDatabase {

    this: HttpOps =>

    val API = "http://api.tvmaze.com/"

    val SHOW_DETAILS = API + "shows/"
    val SHOW_SEARCH = API + "search/shows"

    val EMBED = "embed[]"
    val KITCHEN_SINK = Seq((EMBED, "cast"), (EMBED, "crew"), (EMBED, "seasons"), (EMBED, "episodes"))

    def databaseId = "maze"

    def databaseName = "TV Maze"

    def authenticate(c: Credentials) =
        MazeToken() |> asRight

    def search(name: String, t: Token, lang: String) =
        for {
            results <- maybe(seqResult)(SHOW_SEARCH, Seq(("q", name)))
            searchResults <- convertResults(results)
        } yield searchResults

    private def seqResult(v: JsValue) =
        v.convertTo[SearchResult].items.flatMap { _ show } |> asRight

    private def convertResults(results: Seq[ShowWithEmbeds]) =
        results
            .filter { r => r.id.nonEmpty && r.name.nonEmpty }
            .map {
                // XXX - this language is loooooong
                r => ApiSearchResult(databaseId, r.id get, r.name get, r premiered, r language, r genres)
            } |> asRight

    def getTvSeries(identifier: String, seasons: Option[Set[Int]], t: Token, lang: String) =
        for {
            showId <- asInt(identifier)
            show <- maybe(extractShow)(SHOW_DETAILS + showId, KITCHEN_SINK)
            requiredSeasons = seasons.getOrElse(1 to 99 toSet).toSeq.sorted
            availableSeasons = show._embedded.fold { 1 to 99 toSet } { e => seasonNumbers(e seasons) }
            seq = requiredSeasons filter { availableSeasons contains }
            series <- buildTvSeries(show, seq)
        } yield series

    private def extractShow(v: JsValue) =
        v.convertTo[ShowWithEmbeds] |> asRight

    private def seasonNumbers(s: Option[Seq[Season]]) =
        s.fold(Set.empty[Int]) { _ flatMap { _ number } toSet }

    private def buildTvSeries(show: ShowWithEmbeds, seasons: Seq[Int]) =
        TvSeries(
            airDate = show premiered,
            backdropUrl = None,
            genres = show genres,
            language = show language,
            name = show name,
            numberOfSeasons = show._embedded map (e => seasonNumbers(e seasons) max),
            overview = show.summary map sanitize,
            posterUrl = original(show image),
            rating = show.rating flatMap { _ average },
            runtime = show runtime,
            seasons = Some(buildTvSeasons(show, seasons)),
            status = show status,
            votes = None) |> asRight

    private def sanitize(s: String) =
        s |> tidyHtml |> allTidy

    private def original(i: Option[Images]) =
        i flatMap { _.original }

    private def buildTvSeasons(show: ShowWithEmbeds, seasons: Seq[Int]) = {
        val cast = show._embedded flatMap { _ cast }
        val crew = show._embedded flatMap { _ crew }
        val ss = show._embedded flatMap { _ seasons } getOrElse Nil
        val es = show._embedded flatMap { _ episodes } getOrElse Nil

        seasons flatMap { n =>
            ss find { _.number contains n } map { buildTvSeason(es filter { _.season contains n }, cast, crew) }
        }
    }

    private def buildTvSeason(episodes: Seq[Episode], cast: Option[Seq[Cast]], crew: Option[Seq[Crew]])(season: Season) =
        TvSeason(
            airDate = season premiereDate,
            episodes = Some(episodes map buildTvEpisode(cast, crew)),
            number = season number,
            numberOfEpisodes = season episodeOrder,
            overview = season.summary map sanitize,
            posterUrl = original(season image))

    private def buildTvEpisode(cast: Option[Seq[Cast]], crew: Option[Seq[Crew]])(episode: Episode) =
        TvEpisode(
            airDate = episode airdate,
            cast = cast map castToRole,
            crew = crew map crewToRole,
            name = episode name,
            number = episode number,
            overview = episode.summary map sanitize,
            screenshotUrl = original(episode image),
            rating = None,
            votes = None)

    private def castToRole(cast: Seq[Cast]) =
        cast map { c => ApiRole(c.person flatMap { _ name }, c.character flatMap { _ name }) }

    private def crewToRole(crew: Seq[Crew]) =
        crew map { c => ApiRole(c.person flatMap { _ name }, c `type`) }

}
