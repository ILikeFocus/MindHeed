package com.mindheed.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureGeneratorTest {

    @Test
    fun everyKindFillsTheImageWithOpaqueVariedPixels() {
        for (kind in TextureGenerator.Kind.entries) {
            val pixels = TextureGenerator.generate(60, 80, kind, seed = 7)
            assertEquals(60 * 80, pixels.size)
            assertTrue("$kind must be opaque", pixels.all { (it ushr 24) == 0xFF })
            assertTrue("$kind must not be flat", pixels.toSet().size > 50)
        }
    }

    @Test
    fun sameSeedSameImage_otherSeedOtherImage() {
        val a = TextureGenerator.generate(40, 40, TextureGenerator.Kind.CLOUDS, seed = 1)
        val b = TextureGenerator.generate(40, 40, TextureGenerator.Kind.CLOUDS, seed = 1)
        val c = TextureGenerator.generate(40, 40, TextureGenerator.Kind.CLOUDS, seed = 2)
        assertArrayEquals(a, b)
        assertFalse(a.contentEquals(c))
    }

    @Test
    fun onEnterReportsNewSessions() {
        val tracker = ScrollTracker()
        assertTrue(tracker.onEnter(0L))
        tracker.onLeave(1_000L)
        assertFalse(tracker.onEnter(5_000L))
        tracker.onLeave(6_000L)
        assertTrue(tracker.onEnter(6_000L + ScrollTracker.BREAK_RESET_MS))
    }
}
