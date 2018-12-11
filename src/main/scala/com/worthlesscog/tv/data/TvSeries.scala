package com.worthlesscog.tv.data

// XXX - change dates to ... Dates

case class TvSeries(
    airDate: Option[String] = None,
    backdropUrl: Option[String] = None,
    genres: Option[Seq[String]] = None,
    language: Option[String] = None,
    name: Option[String] = None,
    numberOfSeasons: Option[Int] = None,
    overview: Option[String] = None,
    posterUrl: Option[String] = None,
    rating: Option[Double] = None,
    runtime: Option[Int] = None,
    seasons: Option[Seq[TvSeason]] = None,
    votes: Option[Int] = None
)

// XXX - split cast and crew, crew might need a department
case class Role(
    name: Option[String],
    role: Option[String]
)

case class TvSeason(
    airDate: Option[String] = None,
    episodes: Option[Seq[TvEpisode]] = None,
    number: Option[Int] = None,
    numberOfEpisodes: Option[Int] = None,
    overview: Option[String] = None,
    posterUrl: Option[String] = None
)

case class TvEpisode(
    airDate: Option[String] = None,
    cast: Option[Seq[Role]] = None,
    crew: Option[Seq[Role]] = None,
    name: Option[String] = None,
    number: Option[Int] = None,
    overview: Option[String] = None,
    screenshotUrl: Option[String] = None,
    rating: Option[Double] = None,
    votes: Option[Int] = None
)
