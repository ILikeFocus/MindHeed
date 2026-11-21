package com.mindheed.app

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates the images of the attention pause: clouds, plaster walls, marble and wood grain.
 * Every image is new (random seed), made of noise in which the brain naturally finds shapes
 * (pareidolia). Nothing is downloaded and no photo rights are involved.
 *
 * Pure Kotlin (ARGB pixels), no Android dependency, so it can be unit-tested.
 */
object TextureGenerator {

    enum class Kind { CLOUDS, WALL, MARBLE, WOOD }

    fun generate(width: Int, height: Int, kind: Kind, seed: Int): IntArray {
        val noise = Noise(seed)
        val pixels = IntArray(width * height)
        val scale = 4f / width
        for (py in 0 until height) {
            for (px in 0 until width) {
                val x = px * scale
                val y = py * scale
                pixels[py * width + px] = when (kind) {
                    Kind.CLOUDS -> clouds(noise, x, y, py.toFloat() / height)
                    Kind.WALL -> wall(noise, x, y)
                    Kind.MARBLE -> marble(noise, x, y)
                    Kind.WOOD -> wood(noise, x, y)
                }
            }
        }
        return pixels
    }

    private fun clouds(n: Noise, x: Float, y: Float, v: Float): Int {
        val sky = mix(rgb(52, 120, 205), rgb(160, 205, 240), v)
        val density = smoothstep(0.42f, 0.72f, n.fbm(x * 0.9f, y * 0.9f, 6))
        val shade = 0.78f + 0.22f * n.fbm(x * 1.7f + 40f, y * 1.7f + 40f, 4)
        val cloud = mix(rgb(150, 160, 178), rgb(255, 255, 255), shade)
        return mix(sky, cloud, density)
    }

    private fun wall(n: Noise, x: Float, y: Float): Int {
        val stain = n.fbm(x * 1.4f, y * 1.4f, 6)
        val grain = n.fbm(x * 9f, y * 9f, 3)
        val base = mix(rgb(120, 112, 100), rgb(214, 204, 186), stain * 0.8f + grain * 0.2f)
        // Cracks: thin dark lines where ridged noise peaks.
        val ridge = 1f - abs(2f * n.fbm(x * 0.8f + 90f, y * 0.8f + 90f, 5) - 1f)
        val crack = smoothstep(0.93f, 0.99f, ridge)
        return mix(base, rgb(60, 54, 48), crack * 0.85f)
    }

    private fun marble(n: Noise, x: Float, y: Float): Int {
        val turbulence = n.fbm(x * 1.2f, y * 1.2f, 6)
        val vein = abs(sin((x * 2.2f + y * 1.3f + turbulence * 6f).toDouble())).toFloat()
        val dark = 1f - smoothstep(0.0f, 0.35f, vein)
        return mix(rgb(236, 233, 228), rgb(92, 96, 110), dark * 0.9f)
    }

    private fun wood(n: Noise, x: Float, y: Float): Int {
        val warp = n.fbm(x * 0.7f, y * 0.7f, 5) * 2.4f
        val dx = x - 1.3f
        val dy = (y - 3.5f) * 0.25f
        val rings = sin(((sqrt(dx * dx + dy * dy) + warp) * 16f).toDouble()).toFloat() * 0.5f + 0.5f
        val grain = n.fbm(x * 12f, y * 2f, 3)
        return mix(rgb(96, 58, 32), rgb(196, 140, 90), rings * 0.7f + grain * 0.3f)
    }

    // --- Colour helpers -----------------------------------------------------------------------

    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            return (ca + (cb - ca) * k).toInt().coerceIn(0, 255)
        }
        return rgb(channel(16), channel(8), channel(0))
    }

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Seeded value noise with fractal sum (fBm). Returns values in 0..1. */
    private class Noise(private val seed: Int) {

        fun fbm(x: Float, y: Float, octaves: Int): Float {
            var sum = 0f
            var amplitude = 0.5f
            var frequency = 1f
            var norm = 0f
            repeat(octaves) {
                sum += amplitude * value(x * frequency, y * frequency)
                norm += amplitude
                amplitude *= 0.5f
                frequency *= 2.03f
            }
            return sum / norm
        }

        private fun value(x: Float, y: Float): Float {
            val x0 = floor(x).toInt()
            val y0 = floor(y).toInt()
            val fx = fade(x - x0)
            val fy = fade(y - y0)
            val top = lerp(lattice(x0, y0), lattice(x0 + 1, y0), fx)
            val bottom = lerp(lattice(x0, y0 + 1), lattice(x0 + 1, y0 + 1), fx)
            return lerp(top, bottom, fy)
        }

        private fun lattice(x: Int, y: Int): Float {
            var h = x * 374761393 + y * 668265263 + seed * 1442695041
            h = (h xor (h ushr 13)) * 1274126177
            h = h xor (h ushr 16)
            return (h and 0xFFFFFF) / 16777215f
        }

        private fun fade(t: Float) = t * t * (3f - 2f * t)

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    }
}
