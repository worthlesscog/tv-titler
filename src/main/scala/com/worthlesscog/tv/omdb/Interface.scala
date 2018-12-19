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
    Director: Option[String],
    // Writer: Option[String],
    // Actors: Option[String],
    Plot: Option[String],
    Language: Option[String],
    // Country: Option[String],
    // Awards: Option[String],
    Poster: Option[String],
    Ratings: Option[Seq[Rating]],
    // Metascore: Option[String],
    imdbRating: Option[String],
    imdbVotes: Option[String],
    imdbID: Option[String],
    // Type: Option[String],
    totalSeasons: Option[String],
    Response: Option[String]
)

case class Rating(
    Source: Option[String],
    Value: Option[String]
)
