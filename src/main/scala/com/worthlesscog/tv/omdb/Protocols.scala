package com.worthlesscog.tv.omdb

import spray.json.DefaultJsonProtocol

object Protocols extends DefaultJsonProtocol {

    import com.worthlesscog.tv.optionStringFormat

    implicit val titleSynopsisFmt = jsonFormat5(TitleSynopsis)
    implicit val searchResultFmt = jsonFormat3(SearchResult)

    implicit val titleFmt = jsonFormat10(Title)

    implicit val episodeSynopsisFmt = jsonFormat1(EpisodeSynopsis)
    implicit val seasonFmt = jsonFormat2(Season)

    implicit val episodeFmt = jsonFormat9(Episode)

}
