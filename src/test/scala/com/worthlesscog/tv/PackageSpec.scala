package com.worthlesscog.tv

import com.worthlesscog.tv.data.TvSeries
import org.scalatest._

class PackageSpec extends FreeSpec with Matchers {

    "first" - {
        "should return None for missing fields" in {
            first(Seq(TvSeries()))(_.name) should be(None)
        }
        "should return the first field" in {
            first(Seq(TvSeries(), TvSeries(name = Some("Fred")), TvSeries(name = Some("Wilma"))))(_.name) should be(Some("Fred"))
        }
    }

}
