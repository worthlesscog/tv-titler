package com.worthlesscog.tv.maze

// Search

case class SearchResult(
    items: Seq[ShowResult]
)

case class ShowResult(
    score: Option[Double],
    show: Option[Show]
)

case class Show(
    id: Option[Int],
    // url: Option[String],
    name: Option[String],
    // `type`: Option[String],
    language: Option[String],
    genres: Option[Seq[String]],
    status: Option[String],
    runtime: Option[Int],
    premiered: Option[String],
    // officialSite: Option[String],
    // schedule: Option[Schedule],
    rating: Option[Rating],
    // weight: Option[Int],
    // network: Option[Network],
    // webChannel: Option[String],
    externals: Option[Externals],
    image: Option[Images],
    summary: Option[String] //,
    // updated: Option[Int],
    // _links: Option[Links]
)

case class Rating(
    average: Option[Double]
)

case class Externals(
    tvrage: Option[Int],
    thetvdb: Option[Int],
    imdb: Option[String]
)

case class Images(
    medium: Option[String],
    original: Option[String]
)
