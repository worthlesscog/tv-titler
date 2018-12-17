package com.worthlesscog.tv.data

case class SearchResult(
    source: String,
    id: Int,
    name: String,
    firstAirDate: Option[String],
    language: Option[String],
    genres: Option[Seq[String]]
)
