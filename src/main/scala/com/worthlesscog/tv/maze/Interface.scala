package com.worthlesscog.tv.maze

// Search

case class SearchResult(
    items: Seq[ShowResult]
)

case class ShowResult(
    score: Option[Double],
    show: Option[ShowWithEmbeds]
)

case class ShowWithEmbeds(
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
    // externals: Option[Externals],
    image: Option[Images],
    summary: Option[String],
    // updated: Option[Int],
    // _links: Option[Links],
    _embedded: Option[Embedded]
)

case class Rating(
    average: Option[Double]
)

case class Images(
    medium: Option[String],
    original: Option[String]
)

case class Embedded(
    cast: Option[Seq[Cast]],
    crew: Option[Seq[Crew]],
    seasons: Option[Seq[Season]],
    episodes: Option[Seq[Episode]]
)

// Cast

case class Cast(
    person: Option[Person],
    character: Option[Role] //,
    // self: Option[Boolean],
    // voice: Option[Boolean]
)

case class Person(
    // id: Option[Int],
    // url: Option[String],
    name: Option[String] //,
    // country: Option[Country],
    // birthday: Option[String],
    // deathday: Option[String],
    // gender: Option[Gender],
    // image: Option[Images],
    // _links: Option[Links]
)

case class Role(
    // id: Option[Int],
    // url: Option[String],
    name: Option[String] //,
    // image: Option[Images]
    // _links: Option[Links]
)

// Crew

case class Crew(
    `type`: Option[String],
    person: Option[Person]
)

// Season

case class Season(
    // id: Option[Int],
    // url: Option[String],
    number: Option[Int],
    name: Option[String],
    episodeOrder: Option[Int],
    premiereDate: Option[String],
    // endDate: Option[String],
    // network: Option[Network],
    // webChannel: Option[String],
    image: Option[Images],
    summary: Option[String] //,
    // _links: Option[Links]
)

// Episode

case class Episode(
    // id: Option[Int],
    // url: Option[String],
    name: Option[String],
    season: Option[Int],
    number: Option[Int],
    airdate: Option[String],
    // airtime: Option[String],
    // airstamp: Option[String],
    // runtime: Option[Int],
    image: Option[Images],
    summary: Option[String] //,
    // _links: Option[Links]
)
