package com.mindheed.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Settings, stored only on the phone.
 *
 * Each tunable has two layers: the user's own choice (written when they move a slider) and a
 * default that the remote config can improve over time. Until the user touches a slider, the
 * latest online default applies.
 */
class Prefs(context: Context) {

    val raw: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = raw.getBoolean(KEY_ENABLED, true)
        set(value) = raw.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Posts that stay fully clear before the veil starts (extension: clearPosts). */
    var clearPosts: Int
        get() = raw.getInt(KEY_CLEAR_POSTS, raw.getInt(REMOTE_CLEAR_POSTS, DEFAULT_CLEAR_POSTS))
        set(value) = raw.edit().putInt(KEY_CLEAR_POSTS, value).apply()

    /** Posts for the veil to go from 0 to its maximum (extension: rampPosts). */
    var rampPosts: Int
        get() = raw.getInt(KEY_RAMP_POSTS, raw.getInt(REMOTE_RAMP_POSTS, DEFAULT_RAMP_POSTS))
        set(value) = raw.edit().putInt(KEY_RAMP_POSTS, value).apply()

    /** 0..100 */
    var intensity: Int
        get() = raw.getInt(KEY_INTENSITY, raw.getInt(REMOTE_INTENSITY, DEFAULT_INTENSITY))
        set(value) = raw.edit().putInt(KEY_INTENSITY, value).apply()

    var username: String
        get() = raw.getString(KEY_USERNAME, "") ?: ""
        set(value) = raw.edit().putString(KEY_USERNAME, value).apply()

    /**
     * Wall-clock time until which the veil stays off: set when the bubble opens the saved posts,
     * cleared as soon as the user leaves Instagram. This is the "no limit on Saved" mode.
     */
    var pausedUntil: Long
        get() = raw.getLong(KEY_PAUSED_UNTIL, 0L)
        set(value) = raw.edit().putLong(KEY_PAUSED_UNTIL, value).apply()

    /** Safety cap of the saved-posts mode, in minutes (remote). */
    val savedModeMaxMinutes: Int
        get() = raw.getInt(REMOTE_SAVED_MODE_MAX, DEFAULT_SAVED_MODE_MAX)

    /**
     * Class names of the scrolling views that count as "the main feed" (remote).
     * Empty = every vertical scroll in Instagram counts, until calibrated on a real phone.
     */
    val countOnlyClasses: Set<String>
        get() = raw.getStringSet(REMOTE_COUNT_ONLY_CLASSES, emptySet()) ?: emptySet()

    /** Diagnostic: counters of the current session, shown in the app to calibrate on a real phone. */
    var lastSwipes: Int
        get() = raw.getInt(KEY_LAST_SWIPES, 0)
        set(value) = raw.edit().putInt(KEY_LAST_SWIPES, value).apply()

    var lastItemsPassed: Int
        get() = raw.getInt(KEY_LAST_ITEMS, 0)
        set(value) = raw.edit().putInt(KEY_LAST_ITEMS, value).apply()

    /** Diagnostic: class name and item count of the last scrolled view (no screen content). */
    var lastScrollView: String
        get() = raw.getString(KEY_LAST_SCROLL_VIEW, "") ?: ""
        set(value) = raw.edit().putString(KEY_LAST_SCROLL_VIEW, value).apply()

    companion object {
        private const val FILE = "mindheed"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CLEAR_POSTS = "clear_posts"
        private const val KEY_RAMP_POSTS = "ramp_posts"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_USERNAME = "username"
        private const val KEY_PAUSED_UNTIL = "paused_until"
        const val KEY_LAST_SWIPES = "last_swipes"
        const val KEY_LAST_ITEMS = "last_items"
        const val KEY_LAST_SCROLL_VIEW = "last_scroll_view"

        // Written only by RemoteConfig.
        const val REMOTE_CLEAR_POSTS = "remote_clear_posts"
        const val REMOTE_RAMP_POSTS = "remote_ramp_posts"
        const val REMOTE_INTENSITY = "remote_intensity"
        const val REMOTE_SAVED_MODE_MAX = "remote_saved_mode_max"
        const val REMOTE_COUNT_ONLY_CLASSES = "remote_count_only_classes"
        const val REMOTE_LATEST_VERSION_CODE = "remote_latest_version_code"
        const val REMOTE_LATEST_VERSION_NAME = "remote_latest_version_name"
        const val REMOTE_APK_URL = "remote_apk_url"
        const val REMOTE_FETCHED_AT = "remote_fetched_at"

        // Fully veiled by the 3rd post: 1 clear post, then 2 posts of ramp.
        const val DEFAULT_CLEAR_POSTS = 1
        const val DEFAULT_RAMP_POSTS = 2
        const val DEFAULT_INTENSITY = 70
        const val DEFAULT_SAVED_MODE_MAX = 30
    }
}
