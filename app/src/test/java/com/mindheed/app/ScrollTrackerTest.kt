package com.mindheed.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollTrackerTest {

    /** One swipe = a burst of scroll events 16 ms apart, then the finger lifts. */
    private fun ScrollTracker.swipe(at: Long, fromIndex: Int = ScrollTracker.NO_INDEX): Long {
        for (frame in 0 until 10) {
            onScroll(at + frame * 16L, fromIndex)
        }
        return at + 1_000L
    }

    private fun ScrollTracker.swipes(count: Int, from: Long = 0L): Long {
        var t = from
        repeat(count) { t = swipe(t) }
        return t
    }

    @Test
    fun oneBurstOfEventsIsOneSwipe() {
        val tracker = ScrollTracker()
        tracker.swipe(0)

        assertEquals(1, tracker.swipes)
    }

    @Test
    fun staysClearDuringTheClearPosts() {
        val tracker = ScrollTracker()
        tracker.swipes(3)

        assertEquals(0f, tracker.progress(clearPosts = 3, rampPosts = 9), 0.0001f)
    }

    @Test
    fun reachesFullVeilAfterClearPlusRampPosts() {
        val tracker = ScrollTracker()
        tracker.swipes(12)

        assertEquals(1f, tracker.progress(clearPosts = 3, rampPosts = 9), 0.0001f)
    }

    @Test
    fun usesTheSameEasingAsTheExtension() {
        val tracker = ScrollTracker()
        tracker.swipes(6) // 3 clear + 3 of a 9-post ramp

        val expected = ScrollTracker.easeOutCubic(3f / 9f)
        assertEquals(expected, tracker.progress(clearPosts = 3, rampPosts = 9), 0.0001f)
    }

    @Test
    fun swipingBackUpDoesNotCount() {
        val tracker = ScrollTracker()
        var t = tracker.swipe(0, fromIndex = 10)
        t = tracker.swipe(t, fromIndex = 11)
        tracker.swipe(t, fromIndex = 10) // back up one item

        assertEquals(2, tracker.swipes)
    }

    @Test
    fun countsListItemsPassed() {
        val tracker = ScrollTracker()
        var t = tracker.swipe(0, fromIndex = 0)
        t = tracker.swipe(t, fromIndex = 2)
        tracker.swipe(t, fromIndex = 5)

        assertEquals(5, tracker.itemsPassed)
    }

    @Test
    fun switchingToAnotherListIsNotScrollingUp() {
        val tracker = ScrollTracker()
        var t = tracker.swipe(0, fromIndex = 40)
        t = tracker.swipe(t, fromIndex = 41)
        tracker.swipe(t, fromIndex = 0) // e.g. feed -> Reels

        assertEquals(3, tracker.swipes)
    }

    @Test
    fun quickAppSwitchKeepsTheCounter() {
        val tracker = ScrollTracker()
        val end = tracker.swipes(5)
        tracker.onLeave(end)
        tracker.onEnter(end + 60_000L)

        assertEquals(5, tracker.swipes)
    }

    @Test
    fun realBreakResetsTheCounter() {
        val tracker = ScrollTracker()
        val end = tracker.swipes(5)
        tracker.onLeave(end)
        tracker.onEnter(end + ScrollTracker.BREAK_RESET_MS)

        assertEquals(0, tracker.swipes)
    }

    @Test
    fun onScrollReportsOnlyNewGestures() {
        val tracker = ScrollTracker()
        assertTrue(tracker.onScroll(0))
        assertFalse(tracker.onScroll(16))
        assertTrue(tracker.onScroll(16 + ScrollTracker.GESTURE_GAP_MS + 1))
    }

    @Test
    fun normalizesInstagramUsernames() {
        assertEquals("raphael", Instagram.normalizeUsername("  @raphael/ "))
    }
}
