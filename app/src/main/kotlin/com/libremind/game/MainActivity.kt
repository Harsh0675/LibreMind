package com.libremind.game

import android.animation.ObjectAnimator
import android.app.AlertDialog
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
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private val symbols = listOf("🍎", "🚀", "🌙", "⭐", "🎵", "🐼", "🌈", "🔥", "🍀", "⚡", "🎮", "🦊", "🌻", "🍕", "🐳", "💎", "🧩", "🎯")
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
        window.statusBarColor = Color.rgb(18, 15, 38)
        window.navigationBarColor = Color.rgb(18, 15, 38)
        window.decorView.systemUiVisibility = 0
        showHome()
    }

    private fun showHome() {
        running = false
        handler.removeCallbacks(tick)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            background = gradient(0xFF121026.toInt(), 0xFF2A1D50.toInt())
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(18), d(20), d(24))
        }
        scroll.addView(root)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = text("🧠", 25, false, Color.WHITE).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF7656D8.toInt(), d(18).toFloat())
            elevation = d(3).toFloat()
        }
        brand.addView(logo, LinearLayout.LayoutParams(d(58), d(58)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(14), 0, 0, 0)
        }
        brandText.addView(text("LIBREMIND", 24, true, Color.WHITE))
        brandText.addView(text("Memory • focus • fun", 12, false, 0xFFB9B1D2.toInt()), LinearLayout.LayoutParams(-1, d(22)))
        brand.addView(brandText, LinearLayout.LayoutParams(0, d(58), 1f))
        root.addView(brand)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(d(20), d(20), d(20), d(20))
            background = rounded(0xFF241A45.toInt(), d(24).toFloat())
            elevation = d(2).toFloat()
        }
        hero.addView(text("🧠", 40, false, Color.WHITE))
        hero.addView(text("Train your memory", 22, true, Color.WHITE), lp(0, 34))
        hero.addView(text("Match every pair and build your streak.", 13, false, 0xFFC8C0DD.toInt()), lp(0, 22))
        root.addView(hero, lp(0, 16))

        root.addView(text("CHOOSE YOUR MODE", 12, true, 0xFFAAA1C7.toInt()), lp(0, 34))
        val modes = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modes.addView(modeButton("🧠", "CLASSIC", "8 pairs  •  Relaxed", 0xFF7656D8.toInt()) { startGame(4, false) }, lp(0, 76))
        modes.addView(modeButton("⚡", "TIME RUSH", "8 pairs  •  60 seconds", 0xFF5D4BC9.toInt()) { startGame(4, true) }, lp(0, 12))
        modes.addView(modeButton("🌿", "ZEN MODE", "18 pairs  •  No pressure", 0xFF357A70.toInt()) { startGame(6, false) }, lp(0, 12))
        root.addView(modes)

        val footer = text("OFFLINE  •  NO ACCOUNT  •  NO ADS", 10, true, 0xFF817A9D.toInt()).apply {
            gravity = Gravity.CENTER
        }
        root.addView(footer, lp(0, 42))

        setContentView(scroll)
        fadeIn(scroll)
    }

    private fun modeButton(icon: String, title: String, subtitle: String, color: Int, action: () -> Unit): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(14), d(10), d(14), d(10))
            background = rounded(color, d(20).toFloat())
            elevation = d(4).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animatePress(this)
                handler.postDelayed(action, 110)
            }
        }
        val iconView = text(icon, 24, false, Color.WHITE).apply {
            gravity = Gravity.CENTER
            background = rounded(0x22FFFFFF, d(15).toFloat())
        }
        box.addView(iconView, LinearLayout.LayoutParams(d(52), d(52)))

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(12), 0, 0, 0)
        }
        copy.addView(text(title, 17, true, Color.WHITE))
        copy.addView(text(subtitle, 12, false, 0xFFE6E0F5.toInt()), LinearLayout.LayoutParams(-1, d(22)))
        box.addView(copy, LinearLayout.LayoutParams(0, -1, 1f))
        box.addView(text("›", 30, false, 0xFFF3EEFF.toInt()).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(d(30), d(52)))
        return box
    }

    private fun startGame(size: Int, rush: Boolean) {
        level = size
        timeRush = rush
        val pairs = (size * size) / 2
        val selected = symbols.shuffled().take(pairs)
        cards = (selected + selected).shuffled().toMutableList()
        first = -1
        second = -1
        moves = 0
        matches = 0
        combo = 0
        score = 0
        seconds = 0
        running = true
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 1000)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(12), d(10), d(12), d(12))
            background = gradient(0xFF121026.toInt(), 0xFF21183E.toInt())
        }

        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = smallButton("‹")
        timerText = stat("⏱ 00:00")
        movesText = stat("↔ 0")
        scoreText = stat("★ 0")
        bar.addView(back, LinearLayout.LayoutParams(d(44), d(44)))
        bar.addView(timerText, LinearLayout.LayoutParams(0, d(44), 1f))
        bar.addView(movesText, LinearLayout.LayoutParams(0, d(44), 1f))
        bar.addView(scoreText, LinearLayout.LayoutParams(0, d(44), 1f))
        root.addView(bar)
        back.setOnClickListener { running = false; handler.removeCallbacks(tick); showHome() }

        hintText = text(
            if (rush) "⚡  Match every pair before 60 seconds"
            else if (size == 6) "🌿  Breathe • Focus • Remember"
            else "🧠  Find every matching pair",
            13, true, 0xFFC8C0DD.toInt()
        ).apply { gravity = Gravity.CENTER }
        root.addView(hintText, lp(0, 40))

        val gridHolder = FrameLayout(this).apply {
            foregroundGravity = Gravity.CENTER
        }
        root.addView(gridHolder, LinearLayout.LayoutParams(-1, 0, 1f))

        grid = GridLayout(this).apply {
            columnCount = size
            rowCount = size
            setPadding(d(2), d(2), d(2), d(2))
        }
        gridHolder.addView(grid, FrameLayout.LayoutParams(d(100), d(100), Gravity.CENTER))
        gridHolder.post { resizeGrid(gridHolder) }

        setContentView(root)
        buttons.clear()
        cards.indices.forEach { i ->
            val b = cardButton()
            b.alpha = 0f
            b.setOnClickListener { flip(i) }
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(i % size, 1, 1f)
                rowSpec = GridLayout.spec(i / size, 1, 1f)
                setMargins(d(3), d(3), d(3), d(3))
            })
            buttons.add(b)
            b.animate().alpha(1f).setDuration(180).setStartDelay((i * 10).toLong()).start()
        }
    }

    private fun resizeGrid(holder: FrameLayout) {
        val side = min(holder.width - d(8), holder.height - d(8)).coerceAtLeast(d(220))
        val params = grid.layoutParams as FrameLayout.LayoutParams
        params.width = side
        params.height = side
        params.gravity = Gravity.CENTER
        grid.layoutParams = params
    }

    private fun cardButton() = text("✦", 24, true, 0xFFE7DDFF.toInt()).apply {
        gravity = Gravity.CENTER
        background = rounded(0xFF34285A.toInt(), d(14).toFloat())
        elevation = d(2).toFloat()
        isClickable = true
        isFocusable = true
        stateListAnimator = null
        setPadding(0, 0, 0, 0)
    }

    private fun flip(index: Int) {
        if (!running || index == first || !buttons[index].isEnabled || second != -1) return
        buttons[index].performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        animateCardTap(buttons[index])
        reveal(index)
        if (first == -1) {
            first = index
            return
        }

        second = index
        moves++
        movesText.text = "↔ $moves"
        if (cards[first] == cards[second]) {
            val a = first
            val b = second
            buttons[a].isEnabled = false
            buttons[b].isEnabled = false
            matches++
            combo++
            score += 100 + combo * 20
            scoreText.text = "★ $score"
            hintText.text = if (combo >= 2) "🔥  COMBO ×$combo   +${100 + combo * 20}" else "✨  MATCH!   +${100 + combo * 20}"
            buttons[a].animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).withEndAction { buttons[a].animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
            buttons[b].animate().scaleX(1.06f).scaleY(1.06f).setDuration(100).withEndAction { buttons[b].animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
            first = -1
            second = -1
            if (matches == cards.size / 2) handler.postDelayed({ finishGame() }, 220)
        } else {
            combo = 0
            hintText.text = "Remember their positions…"
            val a = first
            val b = second
            handler.postDelayed({
                hide(a)
                hide(b)
                first = -1
                second = -1
                hintText.text = if (timeRush) "⚡  Match every pair before 60 seconds" else if (level == 6) "🌿  Breathe • Focus • Remember" else "🧠  Find every matching pair"
            }, 700)
        }
    }

    private fun reveal(index: Int) {
        val v = buttons[index]
        v.animate().scaleX(0.78f).setDuration(80).withEndAction {
            v.text = cards[index]
            v.setTextColor(Color.WHITE)
            v.background = rounded(0xFF7656D8.toInt(), d(14).toFloat())
            v.animate().scaleX(1f).setDuration(150).setInterpolator(DecelerateInterpolator()).start()
        }.start()
    }

    private fun hide(index: Int) {
        if (index !in buttons.indices || !buttons[index].isEnabled) return
        val v = buttons[index]
        v.animate().scaleX(0.78f).setDuration(80).withEndAction {
            v.text = "✦"
            v.setTextColor(0xFFE7DDFF.toInt())
            v.background = rounded(0xFF34285A.toInt(), d(14).toFloat())
            v.animate().scaleX(1f).setDuration(150).start()
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
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("PLAY AGAIN") { _, _ -> startGame(level, timeRush) }
            .setNegativeButton("HOME") { _, _ -> showHome() }
            .show()
    }

    private fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun gradient(start: Int, end: Int) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(start, end)
    )

    private fun text(s: String, sp: Int, bold: Boolean, color: Int) = TextView(this).apply {
        text = s
        textSize = sp.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = true
    }

    private fun stat(s: String) = text(s, 13, true, Color.WHITE).apply {
        gravity = Gravity.CENTER
        background = rounded(0xFF2C2447.toInt(), d(15).toFloat())
        setPadding(d(2), 0, d(2), 0)
    }

    private fun smallButton(s: String) = text(s, 29, false, Color.WHITE).apply {
        gravity = Gravity.CENTER
        background = rounded(0xFF2C2447.toInt(), d(15).toFloat())
        isClickable = true
        isFocusable = true
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(if (w == 0) -1 else d(w), d(h))

    private fun d(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun animatePress(view: View) {
        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
        }.start()
    }

    private fun animateCardTap(view: View) {
        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(45).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun fadeIn(view: View) {
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }
}
