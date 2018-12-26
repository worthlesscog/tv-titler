package com.worthlesscog.tv

import org.apache.commons.text.StringEscapeUtils

object TextUtils {

    def allTidy(s: String) = s |>
        unescapeXml |>
        removeControlCharacters |>
        removeGuestStars |>
        removeNamedStars |>
        removeVoiceOf |>
        compressMultiDashes |>
        compressMultiQuotes |>
        compressMultiSpaces |>
        unfloatCommas |>
        unfloatFullStops |>
        unfloatSemiColons |>
        trim

    def compressMultiDashes(s: String) = s.replaceAll("-{2,}", "-")
    def compressMultiSpaces(s: String) = s.replaceAll(" {2,}", " ")
    def compressMultiQuotes(s: String) = s.replaceAll(""""{2,}""", """"""")

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
