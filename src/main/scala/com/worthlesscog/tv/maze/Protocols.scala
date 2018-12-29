package com.worthlesscog.tv.maze

import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object Protocols extends DefaultJsonProtocol {

    implicit val imagesFmt = jsonFormat2(Images)
    implicit val personFmt = jsonFormat1(Person)
    implicit val ratingFmt = jsonFormat1(Rating)
    implicit val roleFmt = jsonFormat1(Role)

    implicit val castFmt = jsonFormat2(Cast)
    implicit val crewFmt = jsonFormat2(Crew)
    implicit val episodesFmt = jsonFormat6(Episode)
    implicit val seasonsFmt = jsonFormat6(Season)

    implicit val embeddedFmt = jsonFormat4(Embedded)
    implicit val showFmt = jsonFormat11(ShowWithEmbeds)
    implicit val showResultFmt = jsonFormat2(ShowResult)

    implicit object searchResultFmt extends RootJsonFormat[SearchResult] {
        def read(value: JsValue) = SearchResult(value.convertTo[List[ShowResult]])
        def write(f: SearchResult) = ???
    }

}
