package dev.flightlink

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity used as a share target. It converts the shared text,
 * opens the other app, and finishes.
 */
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = sharedText(intent)
        val conversion = text?.let { LinkConverter.convertText(it) }

        if (conversion == null) {
            Toast.makeText(this, R.string.error_no_link, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val targetName = getString(
            if (conversion.source == Conversion.Source.FLIGHTRADAR24) {
                R.string.skycards
            } else {
                R.string.fr24
            }
        )

        val result = AppLauncher.open(this, conversion)
        when (result.outcome) {
            AppLauncher.Outcome.OPENED_APP -> {
                Toast.makeText(
                    this,
                    getString(R.string.toast_opening, targetName),
                    Toast.LENGTH_SHORT
                ).show()
            }

            AppLauncher.Outcome.OPENED_BROWSER -> {
                Toast.makeText(this, R.string.toast_browser, Toast.LENGTH_SHORT).show()
            }

            AppLauncher.Outcome.STORE_REQUIRED -> {
                Toast.makeText(
                    this,
                    getString(R.string.toast_store, targetName),
                    Toast.LENGTH_SHORT
                ).show()
            }

            AppLauncher.Outcome.FAILED -> {
                copyToClipboard(conversion.targetUrl)
                Toast.makeText(this, R.string.toast_failed_copied, Toast.LENGTH_LONG).show()
            }
        }

        finish()
    }

    private fun sharedText(intent: Intent?): String? {
        if (intent == null) return null

        val plain = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        val html = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
        val data = intent.dataString

        return listOfNotNull(plain, html, data).firstOrNull { it.isNotBlank() }
    }

    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), value))
    }
}
