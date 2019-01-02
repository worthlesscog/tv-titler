package com.worthlesscog.tv

import com.worthlesscog.tv.TextUtils._
import org.scalatest._

class TextUtilsSpec extends FreeSpec with Matchers {

    "actorCameo" - {
        "should replace bracketed cameo with a space" in {
            actorCameo("Not(Bob in a cameo role)Jim") should be("Not Jim")
        }
    }

    "actorGuest" - {
        "should remove bracketed guest star details" in {
            actorGuest("Not (guest star Bob)Jim") should be("Not Jim")
        }
    }

    "actorNamed" - {
        "should remove bracketed Actor Names" in {
            actorNamed("Not (Bob Smith from Firefly)Jim") should be("Not Jim")
        }
    }

    "actorVoice" - {
        "should remove bracketed voice actor details" in {
            actorVoice("Not (voice of Bob Smith)Jim") should be("Not Jim")
        }
    }

    "compressEmdashes" - {
        "should remove spaces around emdashes" in {
            compressEmdashes("Stop doing — this") should be("Stop doing—this")
        }
    }

    "compressMultiSpaces" - {
        "should replace multiple spaces with a single" in {
            compressMultiSpaces("Too     many  spaces") should be("Too many spaces")
        }
    }

    "compressMultiQuotes" - {
        "should replace multiple quotes with a single" in {
            compressMultiQuotes(""" Too many ""quotes"" """) should be(""" Too many "quotes" """)
        }
    }

    "convertEmdashes" - {
        "should substitute hyphens for emdashes" in {
            convertEmdashes(" - ") should be(" - ")
            convertEmdashes(" -- ") should be(" — ")
            convertEmdashes(" --- ") should be(" — ")
        }
    }

    "convertEndashes" - {
        "should substitute floating hypens or endashes for emdashes" in {
            convertEndashes(" - ") should be("—")
            convertEndashes(" – ") should be("—")
        }
    }

    "htmlItalics" - {
        "should replace italics tagging with quotes" in {
            htmlItalics(" <i>Banana</i> ") should be(""" "Banana" """)
        }
    }

    "htmlTags" - {
        "should replace html tags with spaces" in {
            htmlTags(" <i>Banana</i> <br/>") should be("  Banana   ")
        }
    }

    "removeControlCharacters" - {
        "should replace html tags with spaces" in {
            removeControlCharacters(" Banana\rSplit ") should be(" BananaSplit ")
        }
    }

    "substituteDoubleQuotes" - {
        "should replace flat quotes with 66's and 99's" in {
            substituteDoubleQuotes(""" "Banana" "Split" """) should be(" “Banana” “Split” ")
        }
    }

    "substituteEllipses" - {
        "should replace 3 periods with an ellipsis" in {
            substituteEllipses(" Banana...Split ") should be(" Banana…Split ")
        }
    }

    "substituteLinefeeds" - {
        "should replace linefeeds with a space" in {
            substituteLinefeeds(" Banana\nSplit ") should be(" Banana Split ")
        }
    }

}
