package org.asr.studio.speech

/**
 * Offline, zero-dependency SSML naturaliser.
 * Converts plain robot command text into SSML markup that Android TTS
 * (via TextToSpeech.speak(..., TextToSpeech.QUEUE_FLUSH, null, utteranceId))
 * interprets for more human-sounding speech.
 *
 * Usage: SpeechNaturaliser.naturalise("Warning. Battery critical.")
 * Returns: "<speak><prosody rate='95%'>...</prosody></speak>"
 */
object SpeechNaturaliser {

    fun naturalise(raw: String): String {
        var text = raw.trim()
        text = expandContractions(text)
        text = expandAbbreviations(text)
        text = expandUnits(text)
        text = wrapNumbers(text)
        text = injectEmphasis(text)
        text = injectPauses(text)
        return "<speak><prosody rate=\"95%\" pitch=\"-1st\">$text</prosody></speak>"
    }

    /** Expand common robotic contractions to natural English */
    private fun expandContractions(text: String): String {
        var s = text
        val contractions = mapOf(
            Regex("\\bI am\\b", RegexOption.IGNORE_CASE) to "I'm",
            Regex("\\bI will\\b", RegexOption.IGNORE_CASE) to "I'll",
            Regex("\\bI have\\b", RegexOption.IGNORE_CASE) to "I've",
            Regex("\\bdo not\\b", RegexOption.IGNORE_CASE) to "don't",
            Regex("\\bcannot\\b", RegexOption.IGNORE_CASE) to "can't",
            Regex("\\bwill not\\b", RegexOption.IGNORE_CASE) to "won't",
            Regex("\\bshould not\\b", RegexOption.IGNORE_CASE) to "shouldn't",
            Regex("\\bthat is\\b", RegexOption.IGNORE_CASE) to "that's",
            Regex("\\bit is\\b", RegexOption.IGNORE_CASE) to "it's",
            Regex("\\bthere is\\b", RegexOption.IGNORE_CASE) to "there's",
        )
        contractions.forEach { (pattern, replacement) -> s = pattern.replace(s, replacement) }
        return s
    }

    /** Expand robotics-domain abbreviations */
    private fun expandAbbreviations(text: String): String {
        var s = text
        val abbreviations = mapOf(
            Regex("\\bbtw\\b", RegexOption.IGNORE_CASE) to "by the way",
            Regex("\\bfyi\\b", RegexOption.IGNORE_CASE) to "for your information",
            Regex("\\bLTR\\b") to "later",
            Regex("\\bASAP\\b") to "as soon as possible",
            Regex("\\bETD\\b") to "estimated time of departure",
            Regex("\\bETA\\b") to "estimated time of arrival",
        )
        abbreviations.forEach { (pattern, replacement) -> s = pattern.replace(s, replacement) }
        return s
    }

    /** Expand physical units to their spoken forms */
    private fun expandUnits(text: String): String {
        var s = text
        val units = listOf(
            Regex("m/s2") to "metres per second squared",
            Regex("m/s") to "metres per second",
            Regex("rad/s") to "radians per second",
            Regex("km/h") to "kilometres per hour",
            Regex("\\bm\\b") to "metres",
            Regex("\\bcm\\b") to "centimetres",
            Regex("\\bmm\\b") to "millimetres",
            Regex("\\bHz\\b") to "hertz",
            Regex("\\bkHz\\b") to "kilohertz",
            Regex("\\bμT\\b") to "microtesla",
            Regex("\\bdBFS\\b") to "decibels full scale",
            Regex("\\bdB\\b") to "decibels",
            Regex("([0-9]+(?:\\.[0-9]+)?)\\s*%") to { m: MatchResult -> "${m.groupValues[1]} percent" },
            Regex("([0-9]+(?:\\.[0-9]+)?)\\s*°") to { m: MatchResult -> "${m.groupValues[1]} degrees" },
        )
        // Handle lambda replacements separately
        s = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*%").replace(s) { m -> "${m.groupValues[1]} percent" }
        s = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*°").replace(s) { m -> "${m.groupValues[1]} degrees" }
        s = s.replace("m/s2", "metres per second squared")
        s = s.replace("m/s", "metres per second")
        s = s.replace("rad/s", "radians per second")
        s = s.replace("km/h", "kilometres per hour")
        s = Regex("\\bHz\\b").replace(s, "hertz")
        s = Regex("\\bkHz\\b").replace(s, "kilohertz")
        s = Regex("\\bμT\\b").replace(s, "microtesla")
        s = Regex("\\bdBFS\\b").replace(s, "decibels full scale")
        s = Regex("\\bdB\\b").replace(s, "decibels")
        return s
    }

    /**
     * Wrap standalone numbers with SSML cardinal/ordinal say-as tags.
     * Ordinals: 1st, 2nd, 3rd, 4th...
     * Cardinals: plain integers or decimals
     */
    private fun wrapNumbers(text: String): String {
        var s = text
        // Ordinals first (1st, 2nd, 3rd, 4th, etc.)
        s = Regex("\\b([0-9]+)(st|nd|rd|th)\\b").replace(s) {
            "<say-as interpret-as=\"ordinal\">${it.groupValues[1]}${it.groupValues[2]}</say-as>"
        }
        // Waypoint / route numbers: "waypoint 3", "route 7"
        s = Regex("\\b(waypoint|route|step|node|position|zone)\\s+([0-9]+)\\b", RegexOption.IGNORE_CASE).replace(s) {
            "${it.groupValues[1]} <say-as interpret-as=\"cardinal\">${it.groupValues[2]}</say-as>"
        }
        return s
    }

    /** Wrap warning/critical/alert words with emphasis */
    private fun injectEmphasis(text: String): String {
        var s = text
        val emphasisWords = listOf("warning", "critical", "alert", "danger", "emergency", "urgent", "stop", "halt", "error")
        emphasisWords.forEach { word ->
            s = Regex("\\b$word\\b", RegexOption.IGNORE_CASE).replace(s) {
                "<emphasis level=\"strong\">${it.value}</emphasis>"
            }
        }
        return s
    }

    /** Replace sentence-ending punctuation with SSML breaks for natural pacing */
    private fun injectPauses(text: String): String {
        var s = text
        // Period followed by space or end of string → strong pause
        s = Regex("\\.\\s+").replace(s, ". <break time=\"350ms\"/> ")
        // Comma → short pause
        s = Regex(",\\s+").replace(s, ", <break time=\"150ms\"/> ")
        // Semicolon → medium pause
        s = Regex(";\\s+").replace(s, "; <break time=\"250ms\"/> ")
        // Exclamation → pause after
        s = Regex("!\\s+").replace(s, "! <break time=\"300ms\"/> ")
        return s
    }

    /** Preview the naturalised output for UI display — strips SSML tags for readability */
    fun preview(raw: String): String {
        val ssml = naturalise(raw)
        return Regex("<[^>]+>").replace(ssml, "")
            .replace("  ", " ")
            .trim()
    }
}
