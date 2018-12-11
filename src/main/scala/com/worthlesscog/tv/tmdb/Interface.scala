package com.worthlesscog.tv.tmdb

// Search

case class Genres(
    genres: Option[Seq[Genre]]
)

case class Genre(
    id: Option[Int],
    name: Option[String]
)

case class SearchResult(
    page: Option[Int],
    results: Option[Seq[Result]],
    // total_results: Option[Int],
    total_pages: Option[Int]
)

case class Result(
    // poster_path: Option[String],
    // popularity: Option[Double],
    id: Option[Int],
    // backdrop_path: Option[String],
    // vote_average: Option[Double],
    // overview: Option[String],
    first_air_date: Option[String],
    // origin_country: Option[Seq[String]],
    genre_ids: Option[Seq[Int]],
    original_language: Option[String],
    // vote_count: Option[Int],
    name: Option[String] //,
    // original_name: Option[String]
)

// Config

case class Configuration(
    images: Option[Images] //,
    // change_keys: Option[Seq[String]]
)

case class Images(
    base_url: Option[String],
    // secure_base_url: Option[String],
    backdrop_sizes: Option[Seq[String]],
    // logo_sizes: Option[Seq[String]],
    poster_sizes: Option[Seq[String]],
    // profile_sizes: Option[Seq[String]],
    still_sizes: Option[Seq[String]]
)

case class Languages(
    items: List[Language]
)

case class Language(
    iso_639_1: Option[String],
    english_name: Option[String],
    name: Option[String]
)

// Show

case class Show(
    backdrop_path: Option[String],
    // created_by: Option[Seq[Creator]],
    episode_run_time: Option[Seq[Int]],
    first_air_date: Option[String],
    genres: Option[Seq[Genre]],
    // homepage: Option[String],
    id: Option[Int],
    // in_production: Option[Boolean],
    // languages: Option[Seq[String]],
    // last_air_date: Option[String],
    // last_episode_to_air: Option[EpisodeSynopsis],
    name: Option[String],
    // next_episode_to_air: Option[EpisodeSynopsis],
    // networks: Option[Network],
    // number_of_episodes: Option[Int],
    number_of_seasons: Option[Int],
    // origin_country: Option[Seq[String]],
    original_language: Option[String],
    // original_name: Option[String],
    overview: Option[String],
    // popularity: Option[Double],
    poster_path: Option[String],
    // production_companies: Option[Seq[Company]],
    seasons: Option[Seq[SeasonSynopsis]],
    // status: Option[String],
    // `type`: Option[String],
    vote_average: Option[Double],
    vote_count: Option[Int]
)

case class SeasonSynopsis(
    air_date: Option[String],
    episode_count: Option[Int],
    id: Option[Int],
    // name: Option[String],
    // overview: Option[String],
    // poster_path: Option[String],
    season_number: Option[Int]
)

case class ShowImages(
    backdrops: Option[Seq[Image]],
    id: Option[Int],
    posters: Option[Seq[Image]]
)

case class Image(
    aspect_ratio: Option[Double],
    file_path: Option[String],
    // height: Option[Int],
    iso_639_1: Option[String],
    vote_average: Option[Double] //,
    // vote_count: Option[Int],
    // width: Option[Int]
)

// Credits

case class Credits(
    cast: Option[Seq[Cast]],
    crew: Option[Seq[Crew]],
    id: Option[Int]
)

case class Cast(
    character: Option[String],
    // credit_id: Option[String],
    // gender: Option[Int],
    id: Option[Int],
    name: Option[String],
    order: Option[Int] //,
    // profile_path: Option[String]
)

case class Crew(
    // credit_id: Option[String],
    department: Option[String],
    // gender: Option[Int],
    id: Option[Int],
    job: Option[String],
    name: Option[String] //,
    // profile_path: Option[String]
)

// Season

case class Season(
    // _id: Option[String],
    air_date: Option[String],
    episodes: Option[Seq[Episode]],
    // name: Option[String],
    overview: Option[String],
    id: Option[Int],
    poster_path: Option[String],
    season_number: Option[Int]
)

case class Episode(
    air_date: Option[String],
    crew: Option[Seq[Crew]],
    episode_number: Option[Int],
    guest_stars: Option[Seq[Cast]],
    name: Option[String],
    overview: Option[String],
    id: Option[Int],
    // production_code: Option[String],
    // season_number: Option[Int],
    still_path: Option[String],
    vote_average: Option[Double],
    vote_count: Option[Int]
)

case class SeasonImages(
    id: Option[Int],
    posters: Option[Seq[Image]]
)

// case class EpisodeImages(
//     id: Option[Int],
//     stills: Option[Seq[Image]]
// )
