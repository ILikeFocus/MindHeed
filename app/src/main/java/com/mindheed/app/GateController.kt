package com.mindheed.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The attention pause: at the first scroll of the feed in a session, a full-screen card shows a
 * generated image (clouds, wall, marble, wood) and asks "What shape do you see?". There is no
 * wrong answer; naming any shape (or skipping after a short delay) lets the user through.
 */
class GateController(
    private val service: AccessibilityService,
    private val onDone: (shape: String?) -> Unit,
) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())

    private var root: View? = null

    val isShowing: Boolean
        get() = root != null

    fun show() {
        if (root != null) return
        val view = build()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Focusable (no FLAG_NOT_FOCUSABLE) so the keyboard can type the shape's name.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "MindHeed attention pause"
        }
        try {
            windowManager.addView(view, params)
            root = view
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not show the attention pause", e)
            onDone(null) // Never block Instagram because of an overlay error.
        }
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        val view = root ?: return
        root = null
        try {
            windowManager.removeView(view)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not remove the attention pause", e)
        }
    }

    private fun finish(shape: String?) {
        hide()
        onDone(shape)
    }

    private fun build(): View {
        val image = ImageView(service).apply {
            setImageBitmap(randomTexture())
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat() }
            contentDescription = service.getString(R.string.gate_image_description)
        }

        val answer = EditText(service).apply {
            hint = service.getString(R.string.gate_hint)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(150, 255, 255, 255))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            minHeight = dp(48)
        }

        val ok = Button(service).apply {
            text = service.getString(R.string.gate_ok)
            setTextColor(Color.WHITE)
            isAllCaps = false
            setBackgroundResource(R.drawable.bg_button)
            minHeight = dp(48)
            isEnabled = false
            alpha = 0.5f
            setOnClickListener { finish(answer.text.toString().trim().take(MAX_SHAPE_LENGTH)) }
        }

        answer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val filled = !s.isNullOrBlank()
                ok.isEnabled = filled
                ok.alpha = if (filled) 1f else 0.5f
            }
        })
        answer.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE && ok.isEnabled) {
                ok.performClick()
                true
            } else {
                false
            }
        }

        val skip = Button(service).apply {
            text = service.getString(R.string.gate_skip)
            setTextColor(Color.argb(190, 255, 255, 255))
            isAllCaps = false
            setBackgroundColor(Color.TRANSPARENT)
            minHeight = dp(48)
            visibility = View.INVISIBLE
            setOnClickListener { finish(null) }
        }
        handler.postDelayed({ skip.visibility = View.VISIBLE }, SKIP_DELAY_MS)

        val title = TextView(service).apply {
            text = service.getString(R.string.gate_title)
            setTextColor(Color.argb(190, 255, 255, 255))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        val question = TextView(service).apply {
            text = service.getString(R.string.gate_question)
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        return object : LinearLayout(service) {
            // Back does nothing here: the pause ends by naming a shape or skipping.
            override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                if (event.keyCode == KeyEvent.KEYCODE_BACK) true else super.dispatchKeyEvent(event)
        }.apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 18, 42))
            setPadding(dp(24), dp(56), dp(24), dp(32))
            fitsSystemWindows = true
            addView(title, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(image, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = dp(16) })
            addView(question, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(20) })
            addView(answer, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
            addView(ok, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
            addView(skip, LinearLayout.LayoutParams(WRAP, WRAP).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(4)
            })
        }
    }

    private fun randomTexture(): Bitmap {
        val kind = TextureGenerator.Kind.entries.random()
        val pixels = TextureGenerator.generate(TEXTURE_W, TEXTURE_H, kind, Random.nextInt())
        return Bitmap.createBitmap(pixels, TEXTURE_W, TEXTURE_H, Bitmap.Config.ARGB_8888)
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private companion object {
        const val TAG = "MindHeedGate"
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val SKIP_DELAY_MS = 6_000L
        const val MAX_SHAPE_LENGTH = 40
        // Small and upscaled with filtering: soft shapes, fast to generate.
        const val TEXTURE_W = 180
        const val TEXTURE_H = 240
    }
}
