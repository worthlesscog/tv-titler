package com.worthlesscog.tv.omdb

import spray.json.DefaultJsonProtocol

object Protocols extends DefaultJsonProtocol {

    implicit val titleSynopsisFmt = jsonFormat5(TitleSynopsis)
    implicit val searchResultFmt = jsonFormat3(SearchResult)

}
