package dev.flightlink

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var convertButton: Button
    private lateinit var pasteButton: Button
    private lateinit var resultCard: View
    private lateinit var directionView: TextView
    private lateinit var resultUrlView: TextView
    private lateinit var openButton: Button
    private lateinit var copyButton: Button
    private lateinit var aircraftButton: Button
    private lateinit var statusView: TextView

    private var conversion: Conversion? = null
    private var aircraftUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        convertButton = findViewById(R.id.convert)
        pasteButton = findViewById(R.id.paste)
        resultCard = findViewById(R.id.result_card)
        directionView = findViewById(R.id.direction)
        resultUrlView = findViewById(R.id.result_url)
        openButton = findViewById(R.id.open)
        copyButton = findViewById(R.id.copy)
        aircraftButton = findViewById(R.id.open_aircraft)
        statusView = findViewById(R.id.status)

        findViewById<ImageButton>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setConvertEnabled(false)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setConvertEnabled(!s.isNullOrBlank())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        convertButton.setOnClickListener { convert() }
        pasteButton.setOnClickListener { pasteAndConvert() }

        openButton.setOnClickListener {
            conversion?.let { current ->
                showOpenResult(
                    result = AppLauncher.open(this, current),
                    target = current.targetSource,
                    attemptedUrl = current.targetUrl
                )
            }
        }

        copyButton.setOnClickListener {
            conversion?.let {
                copyToClipboard(it.targetUrl)
                Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show()
            }
        }

        aircraftButton.setOnClickListener {
            aircraftUrl?.let { url ->
                showOpenResult(
                    result = AppLauncher.openUri(this, url, AppLauncher.FR24_PACKAGES),
                    target = Conversion.Source.FLIGHTRADAR24,
                    attemptedUrl = url
                )
            }
        }
    }

    private fun convert() {
        hideKeyboard()

        val text = input.text?.toString().orEmpty()
        val converted = LinkConverter.convertText(text)

        if (converted == null) {
            conversion = null
            aircraftUrl = null
            resultCard.visibility = View.GONE
            aircraftButton.visibility = View.GONE
            statusView.text = getString(
                if (text.isBlank()) R.string.error_empty else R.string.error_no_link
            )
            return
        }

        conversion = converted

        val sourceName = getString(
            if (converted.source == Conversion.Source.FLIGHTRADAR24) R.string.fr24 else R.string.skycards
        )
        val targetName = getString(
            if (converted.targetSource == Conversion.Source.FLIGHTRADAR24) R.string.fr24 else R.string.skycards
        )

        directionView.text = getString(R.string.direction_format, sourceName, targetName)
        resultUrlView.text = converted.targetUrl
        openButton.text = getString(R.string.action_open, targetName)

        val registration = converted.registration
        if (converted.source == Conversion.Source.SKYCARDS && !registration.isNullOrBlank()) {
            aircraftUrl = "https://www.flightradar24.com/data/aircraft/${registration.lowercase()}"
            aircraftButton.visibility = View.VISIBLE
        } else {
            aircraftUrl = null
            aircraftButton.visibility = View.GONE
        }

        statusView.text = ""
        resultCard.visibility = View.VISIBLE
    }

    private fun pasteAndConvert() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(this)?.toString()
        } else {
            null
        }

        if (text.isNullOrBlank()) {
            statusView.text = getString(R.string.error_clipboard)
            return
        }

        input.setText(text)
        input.setSelection(input.text.length)
        convert()
    }

    private fun showOpenResult(
        result: AppLauncher.Result,
        target: Conversion.Source,
        attemptedUrl: String
    ) {
        val targetName = getString(
            if (target == Conversion.Source.FLIGHTRADAR24) R.string.fr24 else R.string.skycards
        )

        statusView.text = when (result.outcome) {
            AppLauncher.Outcome.OPENED_APP -> getString(R.string.status_opened_app, targetName)
            AppLauncher.Outcome.OPENED_BROWSER -> getString(R.string.status_opened_browser)
            AppLauncher.Outcome.STORE_REQUIRED -> getString(R.string.status_store, targetName)
            AppLauncher.Outcome.FAILED -> {
                copyToClipboard(attemptedUrl)
                getString(R.string.status_failed)
            }
        }
    }

    private fun setConvertEnabled(enabled: Boolean) {
        convertButton.isEnabled = enabled
        convertButton.alpha = if (enabled) 1f else 0.45f
    }

    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), value))
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }
}
