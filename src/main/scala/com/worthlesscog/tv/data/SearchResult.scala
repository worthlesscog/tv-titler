package com.worthlesscog.tv.data

case class SearchResult(
    id: Int,
    name: Option[String],
    firstAirDate: Option[String],
    language: Option[String],
    genres: Option[Seq[String]]
)
