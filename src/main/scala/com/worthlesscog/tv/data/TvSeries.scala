package com.worthlesscog.tv.data

// XXX - change dates to ... Dates

trait AirDate {
    def airDate: Option[String]
}

trait Name {
    def name: Option[String]
}

trait Number {
    def number: Option[Int]
}

trait Overview {
    def overview: Option[String]
}

trait PosterUrl {
    def posterUrl: Option[String]
}

trait Rating {
    def rating: Option[Double]
}

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
    status: Option[String] = None,
    votes: Option[Int] = None
) extends AirDate with Name with Overview with PosterUrl with Rating

// XXX - split cast and crew, crew might need a department
case class Role(
    name: Option[String],
    role: Option[String]
) extends Name

case class TvSeason(
    airDate: Option[String] = None,
    episodes: Option[Seq[TvEpisode]] = None,
    number: Option[Int] = None,
    numberOfEpisodes: Option[Int] = None,
    overview: Option[String] = None,
    posterUrl: Option[String] = None
) extends AirDate with Number with Overview with PosterUrl

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
) extends AirDate with Name with Number with Overview with Rating
