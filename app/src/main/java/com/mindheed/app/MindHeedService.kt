package com.mindheed.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Watches only two things: which app is in front, and whether Instagram is being scrolled.
 * It cannot read screen content (canRetrieveWindowContent = false in the service config).
 */
class MindHeedService : AccessibilityService() {

    private lateinit var prefs: Prefs
    private lateinit var overlay: OverlayController
    private val tracker = ScrollTracker()
    private var instagramInFront = false
    private var lastScrollView = ""

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> render() }

    /** "As soon as a connection is available": refresh the remote config. */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = RemoteConfig.refreshIfStale(this@MindHeedService)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        overlay = OverlayController(this, ::onFloatyTapped)
        prefs.raw.registerOnSharedPreferenceChangeListener(prefsListener)
        try {
            getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            RemoteConfig.refreshIfStale(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::prefs.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindowChanged(pkg, now)

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> if (pkg == Instagram.PACKAGE) {
                if (!instagramInFront) enterInstagram(now)
                onInstagramScroll(event, now)
            }
        }
    }

    private fun onInstagramScroll(event: AccessibilityEvent, now: Long) {
        // Swiping through the photos of one carousel post is not scrolling the feed.
        if (isHorizontalScroll(event)) return
        // On the saved posts (after the bubble): no limit, nothing counts.
        if (isPaused()) return

        // Event metadata only (the class of the view that scrolled), never its content.
        val viewClass = event.className?.toString().orEmpty()
        val scrollView = "$viewClass · ${event.itemCount}"
        if (scrollView != lastScrollView) {
            lastScrollView = scrollView
            prefs.lastScrollView = scrollView
        }
        if (!isMainFeed(viewClass)) return

        val fromIndex = if (event.itemCount > 0) event.fromIndex else ScrollTracker.NO_INDEX
        if (tracker.onScroll(now, fromIndex)) {
            prefs.lastSwipes = tracker.swipes
            prefs.lastItemsPassed = tracker.itemsPassed
        }
        render()
    }

    /** Empty list = not calibrated yet: every vertical scroll counts. */
    private fun isMainFeed(viewClass: String): Boolean {
        val allowed = prefs.countOnlyClasses
        return allowed.isEmpty() || viewClass in allowed
    }

    /** Only reliable on Android 9+, where scroll events carry their deltas (-1 = unknown). */
    private fun isHorizontalScroll(event: AccessibilityEvent): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val dx = event.scrollDeltaX
        val dy = event.scrollDeltaY
        return dx != UNKNOWN_DELTA && dx != 0 && (dy == 0 || dy == UNKNOWN_DELTA)
    }

    private fun onWindowChanged(pkg: String, now: Long) {
        when {
            pkg == Instagram.PACKAGE -> if (!instagramInFront) enterInstagram(now)
            isTransientWindow(pkg) -> Unit
            instagramInFront -> {
                instagramInFront = false
                tracker.onLeave(now)
                // Leaving Instagram ends the saved-posts mode: next time, the feed counts again.
                prefs.pausedUntil = 0L
                overlay.hide()
            }
        }
    }

    private fun enterInstagram(now: Long) {
        instagramInFront = true
        tracker.onEnter(now)
        if (tracker.swipes == 0 && prefs.lastSwipes != 0) {
            // A real break reset the counter: reset the diagnostic too.
            prefs.lastSwipes = 0
            prefs.lastItemsPassed = 0
        }
        render()
    }

    /** Notification shade, keyboard and MindHeed's own windows do not mean Instagram was left. */
    private fun isTransientWindow(pkg: String): Boolean =
        pkg == packageName ||
            pkg == "com.android.systemui" ||
            pkg == "android" ||
            pkg == currentKeyboardPackage()

    private fun currentKeyboardPackage(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    private fun isPaused(): Boolean = System.currentTimeMillis() < prefs.pausedUntil

    private fun render() {
        if (!::overlay.isInitialized) return

        if (!instagramInFront || !prefs.enabled || isPaused()) {
            overlay.hide()
            return
        }

        overlay.render(
            progress = tracker.progress(prefs.clearPosts, prefs.rampPosts),
            intensity = prefs.intensity / 100f,
        )
    }

    /**
     * The "unlock": open the personal saved posts with no veil and no limit, until the user
     * leaves Instagram (safety cap from the remote config). The counter starts over.
     */
    private fun onFloatyTapped() {
        tracker.reset()
        prefs.lastSwipes = 0
        prefs.lastItemsPassed = 0
        prefs.pausedUntil = System.currentTimeMillis() + prefs.savedModeMaxMinutes * 60_000L
        overlay.hide()

        val username = prefs.username
        if (username.isBlank()) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(MainActivity.EXTRA_NEED_USERNAME, true),
            )
        } else if (!Instagram.openSavedPosts(this, username)) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::prefs.isInitialized) {
            prefs.raw.unregisterOnSharedPreferenceChangeListener(prefsListener)
            try {
                getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                // Was never registered.
            }
        }
        if (::overlay.isInitialized) {
            overlay.hide()
        }
        super.onDestroy()
    }

    private companion object {
        const val UNKNOWN_DELTA = -1
    }
}
