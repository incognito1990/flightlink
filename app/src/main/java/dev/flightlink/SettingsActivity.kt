package dev.flightlink

import android.app.Activity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RadioGroup

class SettingsActivity : Activity() {

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = Settings(this)

        findViewById<ImageButton>(R.id.back).setOnClickListener { finish() }

        val fr24Group = findViewById<RadioGroup>(R.id.fr24_group)
        val skycardsGroup = findViewById<RadioGroup>(R.id.skycards_group)
        val copyBoth = findViewById<CheckBox>(R.id.copy_both)

        fr24Group.check(
            when (settings.actionFromFlightradar24) {
                ShareAction.OPEN -> R.id.fr24_open
                ShareAction.COPY -> R.id.fr24_copy
                ShareAction.ASK -> R.id.fr24_ask
            }
        )
        skycardsGroup.check(
            when (settings.actionFromSkycards) {
                ShareAction.OPEN -> R.id.skycards_open
                ShareAction.COPY -> R.id.skycards_copy
                ShareAction.ASK -> R.id.skycards_ask
            }
        )
        copyBoth.isChecked = settings.copyBothLinks

        fr24Group.setOnCheckedChangeListener { _, checkedId ->
            settings.actionFromFlightradar24 = when (checkedId) {
                R.id.fr24_open -> ShareAction.OPEN
                R.id.fr24_copy -> ShareAction.COPY
                R.id.fr24_ask -> ShareAction.ASK
                else -> return@setOnCheckedChangeListener
            }
        }

        skycardsGroup.setOnCheckedChangeListener { _, checkedId ->
            settings.actionFromSkycards = when (checkedId) {
                R.id.skycards_open -> ShareAction.OPEN
                R.id.skycards_copy -> ShareAction.COPY
                R.id.skycards_ask -> ShareAction.ASK
                else -> return@setOnCheckedChangeListener
            }
        }

        copyBoth.setOnCheckedChangeListener { _, checked ->
            settings.copyBothLinks = checked
        }
    }
}
