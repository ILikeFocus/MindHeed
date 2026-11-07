package com.mindheed.app

/**
 * Counts how far the user scrolls in Instagram, in posts, and turns it into a 0..1 progress.
 *
 * Like the Chrome extension, the veil depends on the number of posts scrolled. MindHeed never
 * reads the screen, so it counts *scroll gestures*: Android sends a burst of scroll events for
 * each swipe, and a pause longer than [GESTURE_GAP_MS] starts a new gesture. In the feed one
 * swipe moves about one post; in Reels one swipe is exactly one reel.
 *
 * It also counts list items passed (the "first visible item" index that Android attaches to
 * scroll events, when Instagram provides it). That second counter is shown in the app so the
 * two can be compared with what was really scrolled on a real phone.
 *
 * Pure Kotlin, no Android dependency, so it can be unit-tested.
 */
class ScrollTracker {

    /** Downward scroll gestures since the last reset. Drives the veil. */
    var swipes: Int = 0
        private set

    /** List items passed since the last reset (diagnostic, 0 if Instagram does not report it). */
    var itemsPassed: Int = 0
        private set

    private var lastEventAt = NONE
    private var lastFromIndex = NO_INDEX
    private var highestIndex = NO_INDEX
    private var leftAt = NONE

    /**
     * @param fromIndex index of the first visible list item, or a negative value when unknown.
     * @return true when this event started a new counted gesture.
     */
    fun onScroll(now: Long, fromIndex: Int = NO_INDEX): Boolean {
        val goingUp = trackIndex(fromIndex)
        val newGesture = lastEventAt == NONE || now - lastEventAt > GESTURE_GAP_MS
        lastEventAt = now

        if (newGesture && !goingUp) {
            swipes++
            return true
        }
        return false
    }

    /** Returns true when the list clearly moved back up. */
    private fun trackIndex(fromIndex: Int): Boolean {
        if (fromIndex < 0) return false

        val previous = lastFromIndex
        lastFromIndex = fromIndex

        if (previous == NO_INDEX) {
            if (highestIndex == NO_INDEX) highestIndex = fromIndex
            return false
        }

        // A big jump back means another list (e.g. feed -> Reels), not scrolling up.
        if (previous - fromIndex > NEW_LIST_JUMP) {
            highestIndex = fromIndex
            return false
        }

        if (fromIndex > highestIndex) {
            itemsPassed += fromIndex - highestIndex
            highestIndex = fromIndex
        }

        return fromIndex < previous
    }

    /** Instagram went to the background. */
    fun onLeave(now: Long) {
        leftAt = now
        lastEventAt = NONE
        lastFromIndex = NO_INDEX
    }

    /** Instagram came back to the front: a real break resets the counter, a quick app switch does not. */
    fun onEnter(now: Long) {
        if (leftAt != NONE && now - leftAt >= BREAK_RESET_MS) {
            reset()
        }
        leftAt = NONE
    }

    fun reset() {
        swipes = 0
        itemsPassed = 0
        lastEventAt = NONE
        lastFromIndex = NO_INDEX
        highestIndex = NO_INDEX
    }

    /** Same formula as the extension: clear posts first, then an eased ramp up to 1. */
    fun progress(clearPosts: Int, rampPosts: Int): Float {
        val clear = clearPosts.coerceAtLeast(0)
        val ramp = rampPosts.coerceAtLeast(1)
        if (swipes <= clear) return 0f
        val raw = ((swipes - clear).toFloat() / ramp).coerceIn(0f, 1f)
        return easeOutCubic(raw)
    }

    companion object {
        /** Scroll events closer than this belong to the same swipe (a fling lasts a few hundred ms). */
        const val GESTURE_GAP_MS = 400L

        /** Leaving Instagram this long counts as a real break. */
        const val BREAK_RESET_MS = 10 * 60_000L

        /** A backwards jump of more items than this means a different list, not scrolling up. */
        const val NEW_LIST_JUMP = 5

        const val NO_INDEX = -1
        private const val NONE = Long.MIN_VALUE

        fun easeOutCubic(value: Float): Float {
            val inverse = 1f - value
            return 1f - inverse * inverse * inverse
        }
    }
}
