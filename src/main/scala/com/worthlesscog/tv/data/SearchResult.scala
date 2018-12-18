package com.worthlesscog.tv.data

case class SearchResult[T](
    source: String,
    id: T,
    name: String,
    firstAirDate: Option[String],
    language: Option[String],
    genres: Option[Seq[String]]
)
