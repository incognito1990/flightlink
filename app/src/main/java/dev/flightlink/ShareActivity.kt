package dev.flightlink

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity used as a share target. Depending on the per-direction
 * setting it opens the other app, copies the converted link(s), or asks.
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

        when (Settings(this).actionFor(conversion.source)) {
            ShareAction.OPEN -> {
                open(conversion)
                finish()
            }

            ShareAction.COPY -> {
                copy(conversion)
                finish()
            }

            ShareAction.ASK -> ask(conversion)
        }
    }

    private fun open(conversion: Conversion) {
        val targetName = targetName(conversion)
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
    }

    private fun copy(conversion: Conversion) {
        val both = Settings(this).copyBothLinks
        copyToClipboard(conversion.clipboardText(both))
        Toast.makeText(
            this,
            if (both) R.string.toast_copied_both else R.string.toast_copied,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun ask(conversion: Conversion) {
        val both = Settings(this).copyBothLinks
        val targetName = targetName(conversion)

        val dialog = AlertDialog.Builder(this, R.style.Theme_FlightLink_Dialog)
            .setTitle(R.string.ask_title)
            .setMessage(conversion.clipboardText(both))
            .setPositiveButton(getString(R.string.action_open, targetName)) { _, _ ->
                open(conversion)
            }
            .setNeutralButton(R.string.action_copy) { _, _ ->
                copy(conversion)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener { finish() }
        dialog.show()
    }

    private fun targetName(conversion: Conversion): String {
        return getString(
            if (conversion.targetSource == Conversion.Source.FLIGHTRADAR24) {
                R.string.fr24
            } else {
                R.string.skycards
            }
        )
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
