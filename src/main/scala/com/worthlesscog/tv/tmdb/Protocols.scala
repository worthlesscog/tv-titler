package com.worthlesscog.tv.tmdb

import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object Protocols extends DefaultJsonProtocol {

    import com.worthlesscog.tv.optionStringFormat

    implicit val imagesFmt = jsonFormat4(Images)
    implicit val configurationFmt = jsonFormat1(Configuration)

    implicit val languageFmt = jsonFormat3(Language)

    implicit object languagesFormat extends RootJsonFormat[Languages] {
        def read(value: JsValue) = Languages(value.convertTo[List[Language]])
        def write(f: Languages) = ???
    }

    implicit val castFmt = jsonFormat4(Cast)
    implicit val crewFmt = jsonFormat4(Crew)
    implicit val creditsFmt = jsonFormat3(Credits)

    implicit val genreFmt = jsonFormat2(Genre)
    implicit val genresFmt = jsonFormat1(Genres)

    implicit val imageFmt = jsonFormat4(Image)
    // implicit val episodeImagesFmt = jsonFormat2(EpisodeImages)
    implicit val seasonImagesFmt = jsonFormat2(SeasonImages)
    implicit val showImagesFmt = jsonFormat3(ShowImages)

    implicit val resultFmt = jsonFormat5(Result)
    implicit val searchResultFmt = jsonFormat3(SearchResult)

    implicit val episodeFmt = jsonFormat10(Episode)
    implicit val seasonSynopsisFmt = jsonFormat4(SeasonSynopsis)
    implicit val seasonFmt = jsonFormat6(Season)
    implicit val showFmt = jsonFormat14(Show)

}
