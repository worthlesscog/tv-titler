package com.worthlesscog.tv

import org.apache.commons.text.StringEscapeUtils

object TextUtils {

    def allTidy(s: String) = s |>
        unescapeXml |>
        removeControlCharacters |>
        removeGuestStars |>
        removeNamedStars |>
        removeVoiceOf |>
        // compressMultiDashes |>
        convertEmdashes |>
        convertEndashes |>
        compressEmdashes |>
        compressMultiQuotes |>
        compressMultiSpaces |>
        unfloatCommas |>
        unfloatFullStops |>
        unfloatSemiColons |>
        trim

    // def compressMultiDashes(s: String) = s.replaceAll("-{2,}", "-")
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

    // XXX - supplementary characters
    def removeControlCharacters(s: String) = s filterNot { _ isControl }
    def removeGuestStars(s: String) = s.replaceAll("""\(guest +star +.*?\)""", "")
    def removeNamedStars(s: String) = s.replaceAll("""\(\p{Upper}\p{Lower}+ +\p{Upper}\p{Lower}+.*?\)""", "")
    def removeVoiceOf(s: String) = s.replaceAll("""\(voice +of +.*?\)""", "")

    def trim(s: String) = s.trim

    def unescapeXml(s: String) = StringEscapeUtils.unescapeXml(s)

    def unfloatCommas(s: String) = s.replaceAll(" ,", ",")
    def unfloatFullStops(s: String) = s.replaceAll(""" \.""", """\.""")
    def unfloatSemiColons(s: String) = s.replaceAll(" ;", ";")

}
