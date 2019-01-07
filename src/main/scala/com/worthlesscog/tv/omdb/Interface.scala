package com.worthlesscog.tv.omdb

// Search

case class SearchResult(
    Search: Option[Seq[TitleSynopsis]],
    totalResults: Option[String],
    Response: Option[String]
)

case class TitleSynopsis(
    Title: Option[String],
    Year: Option[String],
    imdbID: Option[String],
    Type: Option[String],
    Poster: Option[String]
)

// Title

case class Title(
    Title: Option[String],
    // Year: Option[String],
    // Rated: Option[String],
    Released: Option[String],
    Runtime: Option[String],
    Genre: Option[String],
    // Director: Option[String],
    // Writer: Option[String],
    // Actors: Option[String],
    Plot: Option[String],
    Language: Option[String],
    // Country: Option[String],
    // Awards: Option[String],
    // Poster: Option[String],
    // Ratings: Option[Seq[Rating]],
    // Metascore: Option[String],
    imdbRating: Option[String],
    imdbVotes: Option[String],
    // imdbID: Option[String],
    // Type: Option[String],
    totalSeasons: Option[String] //,
    // Response: Option[String]
)

// Season

case class Season(
    // Title: Option[String],
    Season: Option[String],
    // totalSeasons: Option[String],
    Episodes: Seq[EpisodeSynopsis],
    // Response: Option[String]
)

case class EpisodeSynopsis(
    // Title: Option[String],
    // Released: Option[String],
    // Episode: Option[String],
    // imdbRating: Option[String],
    imdbID: Option[String]
)

// Episode

case class Episode(
    Title: Option[String],
    // Year: Option[String],
    // Rated: Option[String],
    Released: Option[String],
    Season: Option[String],
    Episode: Option[String],
    // Runtime: Option[String],
    // Genre: Option[String],
    Director: Option[String],
    // Writer: Option[String],
    Actors: Option[String],
    Plot: Option[String],
    // Language: Option[String],
    // Country: Option[String],
    // Awards: Option[String],
    // Poster: Option[String],
    // Ratings: Seq[Rating],
    // Metascore: Option[String],
    imdbRating: Option[String],
    imdbVotes: Option[String] //,
    // imdbID: Option[String],
    // seriesID: Option[String],
    // Type: Option[String],
    // Response: Option[String]
)
