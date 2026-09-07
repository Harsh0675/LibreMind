package com.libremind.game

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.ComponentActivity
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val symbols = listOf("🍎","🚀","🌙","⭐","🎵","🐼","🌈","🔥","🍀","⚡","🎮","🦊","🌻","🍕","🐳","💎","🧩","🎯")
    private val handler = Handler(Looper.getMainLooper())
    private var level = 4
    private var cards = mutableListOf<String>()
    private var buttons = mutableListOf<TextView>()
    private var first = -1
    private var second = -1
    private var moves = 0
    private var matches = 0
    private var combo = 0
    private var score = 0
    private var seconds = 0
    private var running = false
    private var timeRush = false
    private lateinit var timerText: TextView
    private lateinit var movesText: TextView
    private lateinit var scoreText: TextView
    private lateinit var hintText: TextView
    private lateinit var grid: GridLayout

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            seconds++
            timerText.text = "⏱ ${formatTime(seconds)}"
            if (timeRush && seconds >= 60) {
                running = false
                showResult("⏰ TIME'S UP!", "You scored $score points in 60 seconds.")
                return
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(25, 22, 48)
        window.navigationBarColor = Color.rgb(25, 22, 48)
        window.decorView.systemUiVisibility = 0
        showHome()
    }

    private fun showHome() {
        running = false
        handler.removeCallbacks(tick)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 22, 24, 20)
            background = gradient(0xFF15132A.toInt(), 0xFF30225A.toInt())
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = text("✦", 30, true, 0xFFE6DBFF.toInt()).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF453272.toInt(), 22f)
            elevation = 5f
        }
        brand.addView(logo, LinearLayout.LayoutParams(62, 62))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 0, 0, 0) }
        brandText.addView(text("LIBREMIND", 25, true, Color.WHITE))
        brandText.addView(text("Memory, made beautiful.", 13, false, 0xFFBDB7D1.toInt()), LinearLayout.LayoutParams(-1, 24))
        brand.addView(brandText, LinearLayout.LayoutParams(0, 62, 1f))
        root.addView(brand)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(18, 22, 18, 18)
            background = rounded(0x263F2D68, 24f)
        }
        hero.addView(text("🧠", 42, false, Color.WHITE))
        hero.addView(text("Train your memory", 22, true, Color.WHITE), lp(0, 4))
        hero.addView(text("Focus • Match • Improve", 14, false, 0xFFC7C0D9.toInt()), lp(0, 4))
        root.addView(hero, lp(0, 18))

        val modes = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modes.addView(modeButton("🧠", "CLASSIC", "8 pairs  •  Relaxed", 0xFF7656D8.toInt()) { startGame(4, false) }, lp(0, 74))
        modes.addView(modeButton("⚡", "TIME RUSH", "8 pairs  •  60 seconds", 0xFF5B4AC7.toInt()) { startGame(4, true) }, lp(0, 12))
        modes.addView(modeButton("🌿", "ZEN MODE", "18 pairs  •  No pressure", 0xFF3C7B70.toInt()) { startGame(6, false) }, lp(0, 12))
        root.addView(modes)

        root.addView(text("OFFLINE  •  NO ACCOUNT  •  NO ADS", 11, true, 0xFF8F89A7.toInt()), lp(0, 18))
        setContentView(root)
        fadeIn(root)
    }

    private fun modeButton(icon: String, title: String, subtitle: String, color: Int, action: () -> Unit): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 10, 18, 10)
            background = rounded(color, 20f)
            elevation = 6f
            isClickable = true
            isFocusable = true
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animatePress(this)
                handler.postDelayed(action, 100)
            }
        }
        box.addView(text(icon, 25, false, Color.WHITE).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(48, 48))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(12, 0, 0, 0) }
        copy.addView(text(title, 17, true, Color.WHITE))
        copy.addView(text(subtitle, 12, false, 0xFFE4DDF4.toInt()), LinearLayout.LayoutParams(-1, 22))
        box.addView(copy, LinearLayout.LayoutParams(0, -1, 1f))
        box.addView(text("›", 28, false, 0xFFEDE8FA.toInt()).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(32, 48))
        return box
    }

    private fun startGame(size: Int, rush: Boolean) {
        level = size
        timeRush = rush
        val pairs = (size * size) / 2
        val selected = symbols.shuffled().take(pairs)
        cards = (selected + selected).shuffled().toMutableList()
        first = -1; second = -1; moves = 0; matches = 0; combo = 0; score = 0; seconds = 0
        running = true
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 1000)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 12)
            background = gradient(0xFF15132A.toInt(), 0xFF211A40.toInt())
        }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = smallButton("‹")
        timerText = stat("⏱ 00:00")
        movesText = stat("↔ 0")
        scoreText = stat("★ 0")
        bar.addView(back, LinearLayout.LayoutParams(44, 44))
        bar.addView(timerText, LinearLayout.LayoutParams(0, 44, 1f))
        bar.addView(movesText, LinearLayout.LayoutParams(0, 44, 1f))
        bar.addView(scoreText, LinearLayout.LayoutParams(0, 44, 1f))
        root.addView(bar)
        back.setOnClickListener { running = false; handler.removeCallbacks(tick); showHome() }

        hintText = text(if (rush) "⚡  Match every pair before 60 seconds" else if (size == 6) "🌿  Breathe • Focus • Remember" else "🧠  Find every matching pair", 13, true, 0xFFC7C0D9.toInt())
        hintText.gravity = Gravity.CENTER
        root.addView(hintText, lp(0, 36))

        grid = GridLayout(this).apply {
            columnCount = size
            rowCount = size
            setPadding(1, 1, 1, 1)
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        buttons.clear()
        cards.indices.forEach { i ->
            val b = cardButton()
            b.alpha = 0f
            b.setOnClickListener { flip(i) }
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = 0; height = 0
                columnSpec = GridLayout.spec(i % size, 1, 1f)
                rowSpec = GridLayout.spec(i / size, 1, 1f)
                setMargins(3, 3, 3, 3)
            })
            buttons.add(b)
            b.animate().alpha(1f).setDuration(180).setStartDelay((i * 9).toLong()).start()
        }
    }

    private fun cardButton() = text("✦", 24, true, 0xFFDCD0FF.toInt()).apply {
        gravity = Gravity.CENTER
        background = rounded(0xFF3A2E61.toInt(), 15f)
        elevation = 3f
        isClickable = true
        isFocusable = true
        stateListAnimator = null
    }

    private fun flip(index: Int) {
        if (!running || index == first || !buttons[index].isEnabled || second != -1) return
        buttons[index].performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        reveal(index)
        if (first == -1) { first = index; return }
        second = index
        moves++
        movesText.text = "↔ $moves"
        if (cards[first] == cards[second]) {
            val a = first; val b = second
            buttons[a].isEnabled = false; buttons[b].isEnabled = false
            buttons[a].animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).withEndAction { buttons[a].animate().scaleX(1f).scaleY(1f).setDuration(100).start() }.start()
            buttons[b].animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).withEndAction { buttons[b].animate().scaleX(1f).scaleY(1f).setDuration(100).start() }.start()
            matches++; combo++
            score += 100 + combo * 20
            scoreText.text = "★ $score"
            hintText.text = if (combo >= 2) "🔥  COMBO ×$combo   +${100 + combo * 20}" else "✨  MATCH!   +${100 + combo * 20}"
            first = -1; second = -1
            if (matches == cards.size / 2) handler.postDelayed({ finishGame() }, 180)
        } else {
            combo = 0
            hintText.text = "Remember their positions…"
            val a = first; val b = second
            handler.postDelayed({ hide(a); hide(b); first = -1; second = -1; hintText.text = if (timeRush) "⚡  Match every pair before 60 seconds" else "🧠  Find every matching pair" }, 650)
        }
    }

    private fun reveal(index: Int) {
        val v = buttons[index]
        v.animate().scaleX(0.86f).setDuration(80).withEndAction {
            v.text = cards[index]
            v.setTextColor(Color.WHITE)
            v.background = rounded(0xFF7656D8.toInt(), 15f)
            v.animate().scaleX(1f).setDuration(130).setInterpolator(DecelerateInterpolator()).start()
        }.start()
    }

    private fun hide(index: Int) {
        if (index !in buttons.indices) return
        val v = buttons[index]
        v.animate().scaleX(0.86f).setDuration(80).withEndAction {
            v.text = "✦"
            v.setTextColor(0xFFDCD0FF.toInt())
            v.background = rounded(0xFF3A2E61.toInt(), 15f)
            v.animate().scaleX(1f).setDuration(130).start()
        }.start()
    }

    private fun finishGame() {
        running = false
        handler.removeCallbacks(tick)
        score += max(0, 3000 - seconds * 15)
        scoreText.text = "★ $score"
        showResult("🎉  BRILLIANT!", "Time   ${formatTime(seconds)}\nMoves   $moves\nScore   $score\n\nYour memory is getting stronger.")
    }

    private fun showResult(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title).setMessage(message)
            .setPositiveButton("PLAY AGAIN") { _, _ -> startGame(level, timeRush) }
            .setNegativeButton("HOME") { _, _ -> showHome() }.show()
    }

    private fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun gradient(start: Int, end: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply { cornerRadius = 0f }

    private fun text(s: String, sp: Int, bold: Boolean, color: Int) = TextView(this).apply {
        text = s; textSize = sp.toFloat(); setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun stat(s: String) = text(s, 13, true, Color.WHITE).apply {
        gravity = Gravity.CENTER; background = rounded(0xFF30284C.toInt(), 15f); setPadding(2, 0, 2, 0)
    }

    private fun smallButton(s: String) = text(s, 29, false, Color.WHITE).apply {
        gravity = Gravity.CENTER; background = rounded(0xFF30284C.toInt(), 15f); isClickable = true; isFocusable = true
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(if (w == 0) -1 else w, h)

    private fun animatePress(view: View) {
        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(60).withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    private fun fadeIn(view: View) {
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply { duration = 320; interpolator = DecelerateInterpolator(); start() }
    }
}
