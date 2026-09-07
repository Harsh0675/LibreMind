package com.libremind.game

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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
            setPadding(28, 28, 28, 24)
            setBackgroundColor(Color.rgb(25, 22, 48))
        }

        val logo = TextView(this).apply {
            text = "✦"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(215, 195, 255))
            background = rounded(0xFF34275F.toInt(), 28f)
            elevation = 8f
        }
        root.addView(logo, LinearLayout.LayoutParams(76, 76))

        val title = text("LIBREMIND", 32, true, 0xFFFFFFFF.toInt())
        root.addView(title, lp(0, 18))
        val sub = text("Train your memory. Master your mind.", 15, false, 0xFFBDB7D1.toInt())
        root.addView(sub, lp(0, 6))

        val modes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 18, 0, 0)
        }
        modes.addView(modeButton("🧠  CLASSIC", "8 pairs • Relaxed", 0xFF7656D8.toInt()) { startGame(4, false) }, lp(0, 0))
        modes.addView(modeButton("⚡  TIME RUSH", "8 pairs • 60 seconds", 0xFF5B4AC7.toInt()) { startGame(4, true) }, lp(0, 14))
        modes.addView(modeButton("🌿  ZEN MODE", "18 pairs • No pressure", 0xFF3C7B70.toInt()) { startGame(6, false) }, lp(0, 14))
        root.addView(modes, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = text("OFFLINE  •  NO ACCOUNT  •  NO ADS", 12, true, 0xFF8F89A7.toInt())
        root.addView(footer, lp(0, 14))
        setContentView(root)
        fadeIn(root)
    }

    private fun modeButton(title: String, subtitle: String, color: Int, action: () -> Unit): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(22, 15, 22, 15)
            background = rounded(color, 20f)
            elevation = 7f
            isClickable = true
            isFocusable = true
            setOnClickListener {
                animatePress(this)
                handler.postDelayed(action, 90)
            }
        }
        box.addView(text(title, 18, true, Color.WHITE))
        box.addView(text(subtitle, 13, false, 0xFFE3DDF3.toInt()), LinearLayout.LayoutParams(-1, 22))
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
            setPadding(14, 12, 14, 14)
            setBackgroundColor(Color.rgb(25, 22, 48))
        }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = smallButton("‹")
        timerText = stat("⏱ 00:00")
        movesText = stat("↔ 0")
        scoreText = stat("★ 0")
        bar.addView(back, LinearLayout.LayoutParams(46, 46))
        bar.addView(timerText, LinearLayout.LayoutParams(0, 46, 1f))
        bar.addView(movesText, LinearLayout.LayoutParams(0, 46, 1f))
        bar.addView(scoreText, LinearLayout.LayoutParams(0, 46, 1f))
        root.addView(bar)
        back.setOnClickListener { running = false; handler.removeCallbacks(tick); showHome() }

        hintText = text(if (rush) "Match every pair before 60 seconds" else if (size == 6) "Breathe • Focus • Remember" else "Find every matching pair", 13, false, 0xFFBDB7D1.toInt())
        root.addView(hintText, lp(0, 8))
        grid = GridLayout(this).apply {
            columnCount = size
            rowCount = size
            setPadding(2, 4, 2, 4)
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        buttons.clear()
        cards.indices.forEach { i ->
            val b = cardButton()
            b.alpha = 0f
            b.setOnClickListener { flip(i) }
            val params = GridLayout.LayoutParams().apply {
                width = 0; height = 0
                columnSpec = GridLayout.spec(i % size, 1, 1f)
                rowSpec = GridLayout.spec(i / size, 1, 1f)
                setMargins(4, 4, 4, 4)
            }
            grid.addView(b, params)
            buttons.add(b)
            b.animate().alpha(1f).setDuration(220).setStartDelay((i * 12).toLong()).start()
        }
    }

    private fun cardButton(): TextView = TextView(this).apply {
        text = "✦"
        textSize = 25f
        gravity = Gravity.CENTER
        setTextColor(0xFFD9CCFF.toInt())
        background = rounded(0xFF3A2E61.toInt(), 16f)
        elevation = 4f
        isClickable = true
        isFocusable = true
        stateListAnimator = null
    }

    private fun flip(index: Int) {
        if (!running || index == first || !buttons[index].isEnabled || second != -1) return
        animatePress(buttons[index])
        buttons[index].text = cards[index]
        buttons[index].setTextColor(Color.WHITE)
        buttons[index].background = rounded(0xFF7656D8.toInt(), 16f)
        if (first == -1) { first = index; return }
        second = index
        moves++
        movesText.text = "↔ $moves"
        if (cards[first] == cards[second]) {
            buttons[first].isEnabled = false
            buttons[second].isEnabled = false
            buttons[first].alpha = 0.65f
            buttons[second].alpha = 0.65f
            matches++
            combo++
            score += 100 + combo * 20
            scoreText.text = "★ $score"
            hintText.text = if (combo >= 2) "🔥 COMBO ×$combo" else "✨ MATCH!"
            first = -1; second = -1
            if (matches == cards.size / 2) finishGame()
        } else {
            combo = 0
            hintText.text = "Keep going — remember their positions"
            val a = first; val b = second
            handler.postDelayed({
                if (a in buttons.indices && b in buttons.indices) {
                    buttons[a].text = "✦"; buttons[b].text = "✦"
                    buttons[a].setTextColor(0xFFD9CCFF.toInt()); buttons[b].setTextColor(0xFFD9CCFF.toInt())
                    buttons[a].background = rounded(0xFF3A2E61.toInt(), 16f)
                    buttons[b].background = rounded(0xFF3A2E61.toInt(), 16f)
                }
                first = -1; second = -1
                hintText.text = if (timeRush) "Match every pair before 60 seconds" else "Find every matching pair"
            }, 650)
        }
    }

    private fun finishGame() {
        running = false
        handler.removeCallbacks(tick)
        score += max(0, 3000 - seconds * 15)
        scoreText.text = "★ $score"
        showResult("🎉 BOARD CLEARED!", "Time  ${formatTime(seconds)}\nMoves  $moves\nScore  $score\n\nExcellent memory!")
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

    private fun text(s: String, sp: Int, bold: Boolean, color: Int) = TextView(this).apply {
        text = s; textSize = sp.toFloat(); gravity = Gravity.CENTER
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun stat(s: String) = text(s, 14, true, Color.WHITE).apply {
        gravity = Gravity.CENTER
        background = rounded(0xFF30284C.toInt(), 16f)
        setPadding(4, 0, 4, 0)
    }

    private fun smallButton(s: String) = text(s, 30, false, Color.WHITE).apply {
        gravity = Gravity.CENTER
        background = rounded(0xFF30284C.toInt(), 16f)
        isClickable = true
        isFocusable = true
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(if (w == 0) -1 else w, h)

    private fun animatePress(view: View) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(55).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
        }.start()
    }

    private fun fadeIn(view: View) {
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            start()
        }
    }
}
