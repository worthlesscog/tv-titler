package com.worthlesscog.tv

import org.apache.commons.text.StringEscapeUtils

object TextUtils {

    def allTidy(s: String) = s |>
        tidyXml |>
        removeActors |>
        substitutions |>
        tidyDashes |>
        tidyText

    def removeActors(s: String) = s |>
        actorCameo |>
        actorGuest |>
        actorNamed |>
        actorVoice

    def substitutions(s: String) = s |>
        substituteDoubleQuotes |>
        substituteEllipses |>
        substituteLinefeeds

    def tidyDashes(s: String) = s |>
        convertEmdashes |>
        convertEndashes |>
        compressEmdashes

    def tidyHtml(s: String) = s |>
        htmlItalics |>
        htmlTags

    def tidyText(s: String) = s |>
        removeControlCharacters |>
        compressMultiQuotes |>
        compressMultiSpaces |>
        unfloatCommas |>
        unfloatFullStops |>
        unfloatSemiColons |>
        trim

    def tidyXml(s: String) = s |>
        xmlUnescape

    def actorCameo(s: String) = s.replaceAll("""\(.*?cameo.*?\)""", " ")
    def actorGuest(s: String) = s.replaceAll("""\(guest +star +.*?\)""", "")
    def actorNamed(s: String) = s.replaceAll("""\(\p{Upper}\p{Lower}+ +\p{Upper}\p{Lower}+.*?\)""", "")
    def actorVoice(s: String) = s.replaceAll("""\(voice +of +.*?\)""", "")

    def compressEmdashes(s: String) = s.replaceAll(" *— *", "—")
    def compressMultiSpaces(s: String) = s.replaceAll(" {2,}", " ")
    def compressMultiQuotes(s: String) = s.replaceAll(""""{2,}""", """"""")

    // emdash   —
    // en dash  –
    // hyphen   -
    // XXX - guessing 2 or 3 hyphens was supposed to be an em dash
    def convertEmdashes(s: String) = s.replaceAll("-{2,3}", "—")
    // XXX - guessing spaced hyphen/en dash was supposed to be an em dash
    def convertEndashes(s: String) = s.replaceAll(" [-–] ", "—")

    // XXX - arbitrary choice, use italics for quotes, ignore bold etc.
    def htmlItalics(s: String) = s.replaceAll("""</?i>""",""""""")
    def htmlTags(s: String) = s.replaceAll("""<.*?>""", " ")

    // XXX - supplementary characters
    def removeControlCharacters(s: String) = s filterNot { _ isControl }

    // “ 66 and 99 ”
    def substituteDoubleQuotes(s: String) = s.replaceAll(""""([^"]*?)"""","""“$1”""")
    def substituteEllipses(s: String) = s.replaceAll("""\.\.\.""", "…")
    def substituteLinefeeds(s: String) = s.replace('\n', ' ')

    def trim(s: String) = s.trim

    def unfloatCommas(s: String) = s.replaceAll(" ,", ",")
    def unfloatFullStops(s: String) = s.replaceAll(""" \.""", """\.""")
    def unfloatSemiColons(s: String) = s.replaceAll(" ;", ";")

    def xmlUnescape(s: String) = StringEscapeUtils.unescapeXml(s)

}
