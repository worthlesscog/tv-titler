package com.worthlesscog.tv.text

case class Episode(
    id: String,
    season: Int,
    episode: Int
)

case class Job(
    title: String,
    order: Int,
    person: String,
    category: String,
    job: String,
    characters: Seq[String]
)

case class Rating(
    rating: Double,
    count: Int
)

case class Person(
    name: String
)

case class Title(
    title: String,
    runtime: Int,
    genres: Seq[String]
)
