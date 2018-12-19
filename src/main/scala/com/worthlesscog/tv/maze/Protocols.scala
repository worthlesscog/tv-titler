package com.worthlesscog.tv.maze

import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object Protocols extends DefaultJsonProtocol {

    implicit val imagesFmt = jsonFormat2(Images)
    implicit val externalsFmt = jsonFormat3(Externals)
    implicit val ratingFmt = jsonFormat1(Rating)
    implicit val showFmt = jsonFormat10(Show)
    implicit val showResultFmt = jsonFormat2(ShowResult)

    implicit object searchResultFmt extends RootJsonFormat[SearchResult] {
        def read(value: JsValue) = SearchResult(value.convertTo[List[ShowResult]])
        def write(f: SearchResult) = ???
    }

}
