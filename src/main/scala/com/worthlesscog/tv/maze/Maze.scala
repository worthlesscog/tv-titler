package com.worthlesscog.tv.maze

import com.worthlesscog.tv.{asRight, HttpOps, Pipe}
import com.worthlesscog.tv.data.{Credentials, Token, TvDatabase, SearchResult => ApiSearchResult}
import com.worthlesscog.tv.maze.Protocols._
import spray.json.JsValue

class Maze extends TvDatabase {

    this: HttpOps =>

    val API = "http://api.tvmaze.com/"

    val TV_SEARCH = API + "search/shows"

    def databaseId = "maze"

    def databaseName = "TV Maze"

    def authenticate(c: Credentials) =
        MazeToken() |> asRight

    def search(name: String, t: Token, lang: String) =
        for {
            results <- maybe(seqResult)(TV_SEARCH, Seq(("q", name)))
            searchResults <- convertResults(results)
        } yield searchResults

    private def seqResult(v: JsValue) =
        v.convertTo[SearchResult].items.flatMap { _.show } |> asRight

    private def convertResults(results: Seq[Show]) =
        results
            .filter { r => r.id.nonEmpty && r.name.nonEmpty }
            .map {
                // XXX - this language is loooooong
                r => ApiSearchResult(databaseId, r.id.get, r.name.get, r.premiered, r.language, r.genres)
            } |> asRight

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]], t: Token, lang: String) =
        ???

}
