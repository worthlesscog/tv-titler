package com.worthlesscog.tv.tvdb

// Auth

case class Authn(
    apikey: String = ""
)

case class Authz(
    token: String
)

// Search

case class SeriesSearch(
    data: Option[Seq[SeriesResult]]
)

case class SeriesResult(
    // aliases: Option[Seq[String]],
    // banner: Option[String],
    firstAired: Option[String],
    id: Option[Int],
    // network: Option[String],
    overview: Option[String],
    seriesName: Option[String],
    slug: Option[String],
    status: Option[String]
)

// Series

case class SeriesData(
    data: Option[Series],
    errors: Option[JsonErrors]
)

case class Series(
    // added: Option[String],
    // addedBy: Option[String],
    // airsDayOfWeek: Option[String],
    // airsTime: Option[String],
    // aliases: Option[Seq[String]],
    // banner: Option[String],
    firstAired: Option[String],
    genre: Option[Seq[String]],
    id: Option[Int],
    // imdbId: Option[String],
    // lastUpdated: Option[Int],
    // network: Option[String],
    // networkId: Option[String],
    overview: Option[String],
    // rating: Option[String],
    runtime: Option[String],
    // seriesId: Option[String],
    seriesName: Option[String],
    siteRating: Option[Double],
    siteRatingCount: Option[Int],
    // slug: Option[String],
    status: Option[String]
    // zap2itId: Option[String]
)

case class JsonErrors(
    invalidFilters: Option[Seq[String]],
    invalidLanguage: Option[String],
    invalidQueryParams: Option[Seq[String]]
)

// Actors

case class SeriesActors(
    data: Option[Seq[Actor]],
    errors: Option[JsonErrors]
)

case class Actor(
    // id: Option[Int],
    // image: Option[String],
    // imageAdded: Option[String],
    // imageAuthor: Option[Int],
    // lastUpdated: Option[String],
    name: Option[String],
    role: Option[String],
    // seriesId: Option[Int],
    sortOrder: Option[Int]
)

// Episodes

case class SeriesEpisodesSummary(
    data: EpisodesSummary
)

case class EpisodesSummary(
    // airedEpisodes: Option[String],
    airedSeasons: Option[Seq[String]] //,
    // dvdEpisodes: Option[String],
    // dvdSeasons: Option[Seq[String]]
)

case class SeriesEpisodes(
    data: Option[Seq[Episode]],
    errors: Option[JsonErrors],
    links: Option[Links]
)

case class Episode(
    // absoluteNumber: Option[Int],
    airedEpisodeNumber: Option[Int],
    airedSeason: Option[Int],
    // airedSeasonID: Option[Int],
    // airsAfterSeason: Option[Int],
    // airsBeforeEpisode: Option[Int],
    // airsBeforeSeason: Option[Int],
    director: Option[String],
    // directors: Option[Seq[String]],
    // dvdChapter: Option[Int],
    // dvdDiscid: Option[String],
    // dvdEpisodeNumber: Option[Int],
    // dvdSeason: Option[Int],
    episodeName: Option[String],
    filename: Option[String],
    firstAired: Option[String],
    guestStars: Option[Seq[String]],
    // id: Option[Int],
    // imdbId: Option[String],
    // language: Option[Language],
    // lastUpdated: Option[Int],
    // lastUpdatedBy: Option[String],
    overview: Option[String],
    // productionCode: Option[String],
    // seriesId: Option[String],
    // showUrl: Option[String],
    siteRating: Option[Double],
    siteRatingCount: Option[Int] //,
    // thumbAdded: Option[String],
    // thumbAuthor: Option[Int],
    // thumbHeight: Option[String],
    // thumbWidth: Option[String],
    // writers: Option[Seq[String]]
)

// case class Language(
//     episodeName: Option[String],
//     overview: Option[String]
// )

case class Links(
    first: Option[Int],
    last: Option[Int] //,
    // next: Option[Int],
    // previous: Option[Int]
)

// Images

case class ImageSearch(
    data: Option[Seq[ImageResult]],
    errors: Option[JsonErrors]
)

// XXX - posters and series art does not support "graphical" subKey
case class ImageResult(
    fileName: Option[String],
    // id: Option[Int],
    keyType: Option[String],
    // languageId: Option[Int],
    ratingsInfo: Option[Ratings],
    resolution: Option[String],
    subKey: Option[String] //,
    // thumbnail: Option[String]
)

case class Ratings(
    average: Option[Double],
    count: Option[Int]
)
