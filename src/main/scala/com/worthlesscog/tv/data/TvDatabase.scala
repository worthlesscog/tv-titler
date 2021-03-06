package com.worthlesscog.tv.data

import com.worthlesscog.tv.Maybe

trait TvDatabase {

    def databaseId: String

    def databaseName: String

    def authenticate(credentials: Credentials): Maybe[Token]

    def search(name: String, token: Token, lang: String): Maybe[Seq[SearchResult[_]]]

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]], token: Token, lang: String): Maybe[TvSeries]

}
