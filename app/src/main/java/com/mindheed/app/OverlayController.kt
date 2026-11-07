package com.mindheed.app

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws MindHeed over Instagram:
 * - the veil: a full-screen layer that lets touches through, with a real blur on Android 12+
 *   (when the phone allows cross-window blur) and a dim layer everywhere;
 * - the floaty: the extension's star bubble, which opens the saved posts.
 *
 * Uses accessibility overlays, so no "draw over other apps" permission is needed.
 */
class OverlayController(
    private val service: AccessibilityService,
    private val onFloatyTap: () -> Unit,
) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density

    private var veil: View? = null
    private var veilParams: WindowManager.LayoutParams? = null
    private var veilAdded = false

    private var floaty: View? = null
    private var floatyParams: WindowManager.LayoutParams? = null
    private var floatyAdded = false
    private var wanderAnimator: ValueAnimator? = null

    fun render(progress: Float, intensity: Float) {
        if (progress <= 0f || intensity <= 0f) {
            hide()
            return
        }

        renderVeil(progress * intensity)

        if (progress >= FLOATY_SHOW_AT) {
            renderFloaty(progress)
        } else {
            removeFloaty()
        }
    }

    fun hide() {
        removeFloaty()
        removeVeil()
    }

    // --- Veil ---------------------------------------------------------------------------------

    private fun renderVeil(strength: Float) {
        val view = veil ?: View(service).also { veil = it }
        val params = veilParams ?: newVeilParams().also { veilParams = it }

        val maxAlpha = if (isBlurSupported()) DIM_ALPHA_WITH_BLUR else DIM_ALPHA_WITHOUT_BLUR
        view.setBackgroundColor(Color.argb((strength * maxAlpha).roundToInt(), 12, 10, 30))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isBlurSupported()) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.blurBehindRadius = (strength * MAX_BLUR_DP * density).roundToInt()
        }

        veilAdded = addOrUpdate(view, params, veilAdded)
    }

    private fun newVeilParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = "MindHeed veil"
    }

    private fun removeVeil() {
        veilAdded = remove(veil, veilAdded)
    }

    private fun isBlurSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled

    // --- Floaty -------------------------------------------------------------------------------

    private fun renderFloaty(progress: Float) {
        val view = floaty ?: createFloaty().also { floaty = it }
        val params = floatyParams ?: newFloatyParams().also { floatyParams = it }

        val bubble = (view as FrameLayout).getChildAt(0)
        val scale = 0.88f + progress * 0.32f
        bubble.scaleX = scale
        bubble.scaleY = scale

        floatyAdded = addOrUpdate(view, params, floatyAdded)

        if (progress >= FLOATY_WANDER_AT) startWander() else stopWander()
    }

    private fun createFloaty(): View {
        val bubbleSize = dp(FLOATY_BUBBLE_DP)

        val star = ImageView(service).apply {
            setImageResource(R.drawable.ic_floaty)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val bubble = FrameLayout(service).apply {
            setBackgroundResource(R.drawable.bg_floaty)
            elevation = dp(6).toFloat()
            contentDescription = service.getString(R.string.floaty_description)
            isClickable = true
            setOnClickListener { onFloatyTap() }
            addView(
                star,
                FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER),
            )
        }

        // The window is larger than the bubble so scaling and rotation never get clipped.
        return FrameLayout(service).apply {
            addView(bubble, FrameLayout.LayoutParams(bubbleSize, bubbleSize, Gravity.CENTER))
        }
    }

    private fun newFloatyParams(): WindowManager.LayoutParams {
        val size = dp(FLOATY_WINDOW_DP)
        val (screenWidth, screenHeight) = screenSize()
        return WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - size - dp(16)
            y = (screenHeight * 0.55f).roundToInt()
            title = "MindHeed bubble"
        }
    }

    /** Same wandering motion as the extension's floating button, mirrored to start from the right edge. */
    private fun startWander() {
        if (wanderAnimator != null) return
        val params = floatyParams ?: return
        val baseX = params.x
        val baseY = params.y
        val startedAt = SystemClock.uptimeMillis()

        wanderAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val view = floaty ?: return@addUpdateListener
                if (!floatyAdded) return@addUpdateListener

                val elapsed = (SystemClock.uptimeMillis() - startedAt).toFloat()
                val sweep = ((elapsed * 0.06f) % 140f) - 20f
                val waveX = sin(elapsed / 1400f) * 46f + cos(elapsed / 2600f) * 30f
                val waveY = sin(elapsed / 1100f) * 44f + cos(elapsed / 2300f) * 72f
                val rotation = sin(elapsed / 1500f) * 18f + cos(elapsed / 2800f) * 6f

                val (screenWidth, screenHeight) = screenSize()
                val size = params.width
                params.x = (baseX - dp(sweep + waveX)).coerceIn(0, screenWidth - size)
                params.y = (baseY + dp(waveY)).coerceIn(dp(48), screenHeight - size - dp(48))
                (view as FrameLayout).getChildAt(0).rotation = rotation

                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: IllegalArgumentException) {
                    // The bubble was removed between two frames.
                }
            }
            start()
        }
    }

    private fun stopWander() {
        wanderAnimator?.cancel()
        wanderAnimator = null
        floaty?.let { (it as FrameLayout).getChildAt(0).rotation = 0f }
    }

    private fun removeFloaty() {
        stopWander()
        floatyAdded = remove(floaty, floatyAdded)
        // Recreated next time so the bubble starts again from its resting place.
        floatyParams = null
    }

    // --- Helpers ------------------------------------------------------------------------------

    /** Returns whether the view is attached afterwards. */
    private fun addOrUpdate(view: View, params: WindowManager.LayoutParams, added: Boolean): Boolean =
        try {
            if (added) windowManager.updateViewLayout(view, params) else windowManager.addView(view, params)
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not show overlay", e)
            added
        }

    /** Returns whether the view is still attached afterwards (always false). */
    private fun remove(view: View?, added: Boolean): Boolean {
        if (view != null && added) {
            try {
                windowManager.removeView(view)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Could not remove overlay", e)
            }
        }
        return false
    }

    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = service.resources.displayMetrics
            metrics.widthPixels to metrics.heightPixels
        }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private fun dp(value: Float): Int = (value * density).roundToInt()

    private companion object {
        const val TAG = "MindHeedOverlay"

        // Same thresholds as the Chrome extension.
        const val FLOATY_SHOW_AT = 0.55f
        const val FLOATY_WANDER_AT = 0.8f

        const val MAX_BLUR_DP = 24f
        const val DIM_ALPHA_WITH_BLUR = 150f
        const val DIM_ALPHA_WITHOUT_BLUR = 225f

        const val FLOATY_BUBBLE_DP = 64
        const val FLOATY_WINDOW_DP = 88
    }
}
