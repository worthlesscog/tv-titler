package com.worthlesscog.tv.omdb

import com.worthlesscog.tv.{asRight, jsStringToInt, HttpOps, Or, Pipe}
import com.worthlesscog.tv.data.{Credentials, Token, TvDatabase, SearchResult => ApiSearchResult}
import com.worthlesscog.tv.omdb.Protocols._
import spray.json.JsValue

class Omdb extends TvDatabase {

    this: HttpOps =>

    val API = "http://www.omdbapi.com/"

    def databaseId = "omdb"

    def databaseName = "The Open Movie Database"

    def authenticate(c: Credentials) =
        OmdbToken(c.token or "") |> asRight

    def search(name: String, t: Token, lang: String) =
        searchWithImplicits(name)(t)

    private def searchWithImplicits(name: String)(implicit t: Token) =
        for {
            results <- pages(searchResult, continue)(API, auth ++ Seq(("s", name), ("type", "series"), ("r", "json")))
            searchResults <- convertResults(results)
        } yield searchResults

    private def continue(n: Int, j: JsValue) =
        n != jsStringToInt(j, "totalResults", n)

    private def auth(implicit t: Token) =
        Seq(("apikey", t.asInstanceOf[OmdbToken].apiKey))

    private def searchResult(v: JsValue) =
        v.convertTo[SearchResult].Search or Nil

    private def convertResults(results: Seq[TitleSynopsis]) =
        results
            .filter { r => r.imdbID.nonEmpty && r.Title.nonEmpty }
            .map {
                r => ApiSearchResult(databaseId, r.imdbID.get, r.Title.get, r.Year, None, None)
            } |> asRight

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]], token: Token, lang: String) =
        getTvSeriesWithImplicits(identifier, seasonNumbers)(token)

    private def getTvSeriesWithImplicits(identifier: String, seasonNumbers: Option[Set[Int]])(implicit token: Token) =
        ???

}
