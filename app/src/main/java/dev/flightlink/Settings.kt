package dev.flightlink

import android.content.Context

enum class ShareAction {
    OPEN,
    COPY,
    ASK
}

class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("flightlink_settings", Context.MODE_PRIVATE)

    var actionFromFlightradar24: ShareAction
        get() = readAction(KEY_FR24, ShareAction.OPEN)
        set(value) {
            prefs.edit().putString(KEY_FR24, value.name).apply()
        }

    var actionFromSkycards: ShareAction
        get() = readAction(KEY_SKYCARDS, ShareAction.OPEN)
        set(value) {
            prefs.edit().putString(KEY_SKYCARDS, value.name).apply()
        }

    var copyBothLinks: Boolean
        get() = prefs.getBoolean(KEY_COPY_BOTH, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COPY_BOTH, value).apply()
        }

    fun actionFor(source: Conversion.Source): ShareAction {
        return if (source == Conversion.Source.FLIGHTRADAR24) {
            actionFromFlightradar24
        } else {
            actionFromSkycards
        }
    }

    private fun readAction(key: String, fallback: ShareAction): ShareAction {
        val raw = prefs.getString(key, fallback.name) ?: fallback.name
        return runCatching { ShareAction.valueOf(raw) }.getOrDefault(fallback)
    }

    private companion object {
        const val KEY_FR24 = "action_fr24_to_skycards"
        const val KEY_SKYCARDS = "action_skycards_to_fr24"
        const val KEY_COPY_BOTH = "copy_both_links"
    }
}
