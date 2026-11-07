package com.mindheed.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * "Self-improving" without breaking store rules: the app never downloads code. It only reads a
 * small JSON file (thresholds, feed calibration, latest version number) whenever a connection is
 * available, at most every few hours. New code always arrives as a normal update: from the store
 * for store installs, or from the GitHub release page for test installs.
 *
 * Nothing about the user is sent: it is a plain GET of a public file.
 */
object RemoteConfig {

    private const val MIN_INTERVAL_MS = 3 * 60 * 60_000L
    private const val TIMEOUT_MS = 10_000

    @Volatile private var running = false

    fun refreshIfStale(context: Context, force: Boolean = false) {
        val url = BuildConfig.CONFIG_URL
        if (url.isBlank() || running) return
        val prefs = Prefs(context)
        val last = prefs.raw.getLong(Prefs.REMOTE_FETCHED_AT, 0L)
        if (!force && System.currentTimeMillis() - last < MIN_INTERVAL_MS) return

        running = true
        Thread {
            try {
                fetch(url)?.let { apply(prefs, it) }
            } catch (e: Exception) {
                // Offline, blocked or malformed: keep the last known values.
            } finally {
                running = false
            }
        }.start()
    }

    private fun fetch(url: String): JSONObject? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.useCaches = false
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** Values are clamped so that a bad file can never break the app. */
    internal fun apply(prefs: Prefs, json: JSONObject) {
        val edit = prefs.raw.edit()
        json.optIntOrNull("clearPosts")?.let { edit.putInt(Prefs.REMOTE_CLEAR_POSTS, it.coerceIn(0, 30)) }
        json.optIntOrNull("rampPosts")?.let { edit.putInt(Prefs.REMOTE_RAMP_POSTS, it.coerceIn(1, 60)) }
        json.optIntOrNull("intensity")?.let { edit.putInt(Prefs.REMOTE_INTENSITY, it.coerceIn(10, 100)) }
        json.optIntOrNull("savedModeMaxMinutes")?.let {
            edit.putInt(Prefs.REMOTE_SAVED_MODE_MAX, it.coerceIn(1, 240))
        }
        json.optJSONArray("countOnlyClasses")?.let { array ->
            val classes = (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
            edit.putStringSet(Prefs.REMOTE_COUNT_ONLY_CLASSES, classes.toSet())
        }
        json.optIntOrNull("latestVersionCode")?.let { edit.putInt(Prefs.REMOTE_LATEST_VERSION_CODE, it) }
        edit.putString(Prefs.REMOTE_LATEST_VERSION_NAME, json.optString("latestVersionName", ""))
        edit.putString(Prefs.REMOTE_APK_URL, json.optString("apkUrl", ""))
        edit.putLong(Prefs.REMOTE_FETCHED_AT, System.currentTimeMillis())
        edit.apply()
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    // ---- Updates ----

    fun latestVersionName(context: Context): String =
        Prefs(context).raw.getString(Prefs.REMOTE_LATEST_VERSION_NAME, "") ?: ""

    fun isUpdateAvailable(context: Context): Boolean =
        Prefs(context).raw.getInt(Prefs.REMOTE_LATEST_VERSION_CODE, 0) > BuildConfig.VERSION_CODE

    /** Store installs update through their store; test installs through the GitHub release page. */
    fun openUpdate(context: Context) {
        val store = installerPackage(context)
        val uri = if (store != null && store in KNOWN_STORES) {
            Uri.parse("market://details?id=${context.packageName}")
        } else {
            val apk = Prefs(context).raw.getString(Prefs.REMOTE_APK_URL, "").orEmpty()
            Uri.parse(apk.ifBlank { BuildConfig.RELEASES_URL })
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (store != null && store in KNOWN_STORES) intent.setPackage(store)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.RELEASES_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** True when a known app store installed MindHeed (Android then skips the "restricted settings" lock). */
    fun isFromStore(context: Context): Boolean = installerPackage(context) in KNOWN_STORES

    private fun installerPackage(context: Context): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    } catch (e: Exception) {
        null
    }

    private val KNOWN_STORES = setOf(
        "com.android.vending", // Google Play
        "com.sec.android.app.samsungapps", // Galaxy Store
        "com.huawei.appmarket", // AppGallery
        "com.xiaomi.market", // GetApps
        "com.xiaomi.mipicks",
    )
}
