package dev.flightlink

import java.math.BigInteger
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Converts a Flightradar24 link into a Skycards link and vice versa.
 *
 * Flightradar24 puts a hexadecimal flight id in its URLs; Skycards uses the
 * same id in decimal:
 *
 *   https://fr24.com/SDM6325/42693275      (0x42693275)
 *   https://skycards.app/flight/-/1114190453
 *
 * The conversion therefore rewrites the link and converts the id between
 * base 16 and base 10. The optional aircraft type segment is dropped because
 * Skycards accepts "-" in its place.
 */
data class Conversion(
    val source: Source,
    val sourceId: String,
    val targetId: String,
    val targetUrl: String,
    val registration: String? = null
) {
    enum class Source { FLIGHTRADAR24, SKYCARDS }

    val targetSource: Source
        get() = if (source == Source.FLIGHTRADAR24) Source.SKYCARDS else Source.FLIGHTRADAR24

    val skycardsUrl: String
        get() {
            val id = if (source == Source.SKYCARDS) sourceId else targetId
            return "https://skycards.app/flight/-/$id"
        }

    val fr24Url: String
        get() {
            val id = if (source == Source.FLIGHTRADAR24) sourceId else targetId
            return "https://fr24.com/$id"
        }

    fun clipboardText(includeBoth: Boolean): String {
        return if (includeBoth) {
            "Skycards: $skycardsUrl\nFlightradar24: $fr24Url"
        } else {
            targetUrl
        }
    }
}

object LinkConverter {

    private val decimalIdPattern = Regex("""\d{4,}""")
    private val hexIdPattern = Regex("""[0-9a-fA-F]{4,}""")

    private val urlPattern = Regex(
        """(?i)(?<![\w./-])(?:(?:https?://)?(?:[a-z0-9-]+\.)*(?:fr24\.com|flightradar24\.com|skycards\.app)(?:/[^\s<>"'`]*)?|https?://[^\s<>"'`]+)"""
    )

    private val trailingCharacters = charArrayOf(
        '.', ',', ';', ':', '!', '?', ')', ']', '}', '>',
        '"', '\'', '»', '«', '”', '“', '’', '‘'
    )

    private val fr24Hosts = setOf(
        "fr24.com",
        "flightradar24.com",
        "m.flightradar24.com"
    )

    private val skycardsHosts = setOf(
        "skycards.app",
        "share.skycards.app"
    )

    fun convertText(text: String): Conversion? {
        return candidateUrls(text).firstNotNullOfOrNull { convertUrl(it) }
    }

    fun findUrl(text: String): String? {
        return candidateUrls(text).firstOrNull()
    }

    fun convertUrl(url: String): Conversion? {
        val normalized = withScheme(url)
        val uri = try {
            URI(normalized)
        } catch (_: URISyntaxException) {
            return null
        }

        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val source = when (host) {
            in fr24Hosts -> Conversion.Source.FLIGHTRADAR24
            in skycardsHosts -> Conversion.Source.SKYCARDS
            else -> return null
        }

        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val registration = queryParameter(uri.rawQuery, "reg")?.takeIf { it.isNotBlank() }

        return when (source) {
            Conversion.Source.FLIGHTRADAR24 -> {
                val hexId = segments.lastOrNull { it.matches(hexIdPattern) } ?: return null
                val decimalId = BigInteger(hexId, 16).toString(10)
                Conversion(
                    source = source,
                    sourceId = hexId,
                    targetId = decimalId,
                    targetUrl = "https://skycards.app/flight/-/$decimalId",
                    registration = registration
                )
            }

            Conversion.Source.SKYCARDS -> {
                val decimalId = segments.lastOrNull { it.matches(decimalIdPattern) } ?: return null
                val hexId = BigInteger(decimalId, 10).toString(16)
                Conversion(
                    source = source,
                    sourceId = decimalId,
                    targetId = hexId,
                    targetUrl = "https://fr24.com/$hexId",
                    registration = registration
                )
            }
        }
    }

    private fun candidateUrls(text: String): Sequence<String> = sequence {
        for (match in urlPattern.findAll(text)) {
            val candidate = match.value.trimEnd(*trailingCharacters)
            if (candidate.isEmpty()) continue
            val normalized = withScheme(candidate)
            val host = hostOf(normalized) ?: continue
            if (host in fr24Hosts || host in skycardsHosts) {
                yield(normalized)
            }
        }
    }

    private fun withScheme(url: String): String {
        return if (url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
        ) {
            url
        } else {
            "https://$url"
        }
    }

    private fun hostOf(url: String): String? {
        return try {
            URI(url).host?.lowercase()?.removePrefix("www.")
        } catch (_: URISyntaxException) {
            null
        }
    }

    private fun queryParameter(rawQuery: String?, name: String): String? {
        if (rawQuery.isNullOrBlank()) return null

        for (part in rawQuery.split('&')) {
            val separator = part.indexOf('=')
            val key = if (separator >= 0) part.substring(0, separator) else part
            if (key == name) {
                val value = if (separator >= 0) part.substring(separator + 1) else ""
                return try {
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    value
                }
            }
        }
        return null
    }
}
