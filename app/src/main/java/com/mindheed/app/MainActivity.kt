package com.mindheed.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var enableButton: Button
    private lateinit var usernameField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        applySystemBarInsets(findViewById(R.id.root))

        status = findViewById(R.id.status)
        enableButton = findViewById(R.id.enable_button)
        enableButton.setOnClickListener {
            if (isServiceEnabled()) openAccessibilitySettings() else showDisclosure()
        }

        bindSettings()

        findViewById<View>(R.id.xiaomi_card).visibility =
            if (isXiaomiFamily()) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.privacy_button).setOnClickListener { showPrivacy() }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        RemoteConfig.refreshIfStale(this)
        maybeOfferUpdate()
    }

    private var updateOffered = false

    /** Offered once per launch, when the online config announces a newer version. */
    private fun maybeOfferUpdate() {
        if (updateOffered || !RemoteConfig.isUpdateAvailable(this)) return
        updateOffered = true
        AlertDialog.Builder(this)
            .setTitle(R.string.update_title)
            .setMessage(getString(R.string.update_body, RemoteConfig.latestVersionName(this)))
            .setPositiveButton(R.string.update_now) { _, _ -> RemoteConfig.openUpdate(this) }
            .setNegativeButton(R.string.disclosure_decline, null)
            .show()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_NEED_USERNAME, false) == true) {
            Toast.makeText(this, R.string.need_username, Toast.LENGTH_LONG).show()
            usernameField.requestFocus()
        }
    }

    private fun refreshStatus() {
        val enabled = isServiceEnabled()
        status.setText(if (enabled) R.string.status_on else R.string.status_off)
        status.setTextColor(getColor(if (enabled) R.color.mh_ok else R.color.mh_warn))
        enableButton.setText(if (enabled) R.string.btn_manage else R.string.btn_enable)

        findViewById<TextView>(R.id.diagnostic).text =
            getString(R.string.diagnostic, prefs.lastSwipes, prefs.lastItemsPassed) + "\n" +
            getString(R.string.diagnostic_view, prefs.lastScrollView.ifBlank { "–" }, BuildConfig.VERSION_NAME)
    }

    private fun bindSettings() {
        findViewById<Switch>(R.id.enabled_switch).apply {
            isChecked = prefs.enabled
            setOnCheckedChangeListener { _, checked -> prefs.enabled = checked }
        }

        usernameField = findViewById(R.id.username)
        usernameField.setText(prefs.username)
        usernameField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                prefs.username = Instagram.normalizeUsername(s?.toString().orEmpty())
            }
        })

        bindSeekBar(
            seekId = R.id.clear_seek,
            labelId = R.id.clear_label,
            labelRes = R.string.clear_label,
            range = 0..30,
            initial = prefs.clearPosts,
        ) { prefs.clearPosts = it }

        bindSeekBar(
            seekId = R.id.ramp_seek,
            labelId = R.id.ramp_label,
            labelRes = R.string.ramp_label,
            range = 1..60,
            initial = prefs.rampPosts,
        ) { prefs.rampPosts = it }

        bindSeekBar(
            seekId = R.id.intensity_seek,
            labelId = R.id.intensity_label,
            labelRes = R.string.intensity_label,
            range = 10..100,
            initial = prefs.intensity,
        ) { prefs.intensity = it }
    }

    private fun bindSeekBar(
        seekId: Int,
        labelId: Int,
        labelRes: Int,
        range: IntRange,
        initial: Int,
        onChange: (Int) -> Unit,
    ) {
        val seek = findViewById<SeekBar>(seekId)
        val label = findViewById<TextView>(labelId)

        fun showValue(value: Int) {
            label.text = getString(labelRes, value)
            seek.contentDescription = label.text
        }

        seek.min = range.first
        seek.max = range.last
        seek.progress = initial.coerceIn(range)
        showValue(seek.progress)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                showValue(value)
                if (fromUser) onChange(value)
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
    }

    /** Prominent disclosure required by the stores before sending the user to the Accessibility settings. */
    private fun showDisclosure() {
        AlertDialog.Builder(this)
            .setTitle(R.string.disclosure_title)
            .setMessage(R.string.disclosure_body)
            .setPositiveButton(R.string.disclosure_accept) { _, _ -> openAccessibilitySettings() }
            .setNegativeButton(R.string.disclosure_decline, null)
            .show()
    }

    private fun showPrivacy() {
        AlertDialog.Builder(this)
            .setTitle(R.string.privacy_title)
            .setMessage(R.string.privacy_body)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(this, MindHeedService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun isXiaomiFamily(): Boolean {
        val brand = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        return listOf("xiaomi", "redmi", "poco").any { it in brand }
    }

    /** targetSdk 35+ draws edge to edge: keep content clear of the status bar, nav bar and keyboard. */
    private fun applySystemBarInsets(root: View) {
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }
            insets
        }
    }

    companion object {
        const val EXTRA_NEED_USERNAME = "need_username"
    }
}
