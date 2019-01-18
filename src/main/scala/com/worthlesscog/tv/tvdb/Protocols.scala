package com.worthlesscog.tv.tvdb

import spray.json.DefaultJsonProtocol

object Protocols extends DefaultJsonProtocol {

    import com.worthlesscog.tv.optionStringFormat

    implicit val authnFormat = jsonFormat1(Authn)

    implicit val authzFormat = jsonFormat1(Authz)

    implicit val jsonErrorsFormat = jsonFormat3(JsonErrors)

    implicit val ratingsFormat = jsonFormat2(Ratings)
    implicit val imageResultFormat = jsonFormat5(ImageResult)
    implicit val imageSearchFormat = jsonFormat2(ImageSearch)

    implicit val actorFormat = jsonFormat3(Actor)
    implicit val seriesActorsFormat = jsonFormat2(SeriesActors)

    implicit val seriesFormat = jsonFormat10(Series)
    implicit val seriesDataFormat = jsonFormat2(SeriesData)

    implicit val episodesSummaryFormat = jsonFormat1(EpisodesSummary)
    implicit val seriesEpisodesSummaryFormat = jsonFormat1(SeriesEpisodesSummary)

    implicit val linksFormat = jsonFormat2(Links)
    // implicit val languageFormat = jsonFormat2(Language)
    implicit val episodeFormat = jsonFormat10(Episode)
    implicit val seriesEpisodesFormat = jsonFormat3(SeriesEpisodes)

    implicit val seriesResultFormat = jsonFormat6(SeriesResult)
    implicit val seriesSearchFormat = jsonFormat1(SeriesSearch)

}
