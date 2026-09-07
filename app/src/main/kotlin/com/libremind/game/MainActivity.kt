package com.libremind.game

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val symbols = listOf("🍎", "🚀", "🌙", "⭐", "🎵", "🐼", "🌈", "🔥", "🍀", "⚡", "🎮", "🦊", "🌻", "🍕", "🐳", "💎", "🧩", "🎯")
    private val handler = Handler(Looper.getMainLooper())
    private var level = 4
    private var cards = mutableListOf<String>()
    private var buttons = mutableListOf<Button>()
    private var first = -1
    private var second = -1
    private var moves = 0
    private var matches = 0
    private var combo = 0
    private var score = 0
    private var seconds = 0
    private var running = false
    private lateinit var timerText: TextView
    private lateinit var movesText: TextView
    private lateinit var scoreText: TextView
    private lateinit var grid: GridLayout

    private val tick = object : Runnable {
        override fun run() {
            if (running) {
                seconds++
                timerText.text = "⏱ ${formatTime(seconds)}"
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        running = false
        handler.removeCallbacks(tick)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(248, 247, 252))
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(32), dp(28), dp(28))
        }

        val badge = text("MEMORY • OFFLINE", 12, true, 0xFF6750A4)
        val title = text("LIBREMIND", 38, true, 0xFF25232A)
        val sub = text("Train your memory. Beat your best.", 16, false, 0xFF6E6A75)
        root.addView(badge, lp(-1, 32, 0))
        root.addView(title, lp(-1, 54, 10))
        root.addView(sub, lp(-1, 30, 2))

        val play = modeButton("▶  PLAY CLASSIC", "4 × 4  •  8 pairs", 0xFF6750A4)
        val rush = modeButton("⚡  TIME RUSH", "4 × 4  •  Race the clock", 0xFF8A63D2)
        val zen = modeButton("☁  ZEN MODE", "6 × 6  •  18 pairs", 0xFF514C59)

        root.addView(play, lp(-1, 82, 28))
        root.addView(rush, lp(-1, 82, 12))
        root.addView(zen, lp(-1, 82, 12))

        val info = text("No account  •  No ads  •  No internet", 13, false, 0xFF77727D)
        root.addView(info, lp(-1, 30, 30))
        root.addView(text("Simple. Private. Just play.", 13, true, 0xFF6750A4), lp(-1, 30, 4))

        play.setOnClickListener { startGame(4, false) }
        rush.setOnClickListener { startGame(4, true) }
        zen.setOnClickListener { startGame(6, false) }

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun startGame(size: Int, timeRush: Boolean) {
        level = size
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
            setPadding(dp(12), dp(10), dp(12), dp(14))
            setBackgroundColor(0xFFF8F7FC.toInt())
        }

        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = smallButton("‹")
        timerText = statText("⏱ 00:00")
        movesText = statText("Moves 0")
        scoreText = statText("Score 0")
        bar.addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
        bar.addView(timerText, LinearLayout.LayoutParams(0, dp(48), 1f))
        bar.addView(movesText, LinearLayout.LayoutParams(0, dp(48), 1f))
        bar.addView(scoreText, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(bar)
        back.setOnClickListener { showHome() }

        val hint = text(
            if (timeRush) "⚡ Match pairs before time runs out!" else "🧠 Find every matching pair",
            13, false, 0xFF6E6A75
        )
        root.addView(hint, lp(-1, 28, 2))

        grid = GridLayout(this).apply {
            columnCount = size
            rowCount = size
            useDefaultMargins = false
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        grid.post {
            val totalGap = dp(8) * (size - 1)
            val card = ((grid.width - totalGap) / size).coerceAtLeast(dp(42))
            buttons.clear()
            cards.indices.forEach { i ->
                val b = cardButton("?")
                b.setOnClickListener { flip(i) }
                val params = GridLayout.LayoutParams().apply {
                    width = card
                    height = card
                    if (i % size != size - 1) rightMargin = dp(8)
                    if (i / size != size - 1) bottomMargin = dp(8)
                }
                grid.addView(b, params)
                buttons.add(b)
            }
        }
    }

    private fun flip(index: Int) {
        if (!running || index == first || !buttons[index].isEnabled) return
        if (first != -1 && second != -1) return

        buttons[index].text = cards[index]
        buttons[index].background = rounded(0xFFE9DDFB, 18)

        if (first == -1) {
            first = index
            return
        }

        second = index
        moves++
        movesText.text = "Moves $moves"

        if (cards[first] == cards[second]) {
            buttons[first].isEnabled = false
            buttons[second].isEnabled = false
            buttons[first].alpha = 0.55f
            buttons[second].alpha = 0.55f
            matches++
            combo++
            score += 100 + combo * 20
            scoreText.text = "Score $score"
            first = -1
            second = -1
            if (matches == cards.size / 2) finishGame()
        } else {
            combo = 0
            val a = first
            val b = second
            handler.postDelayed({
                if (a >= 0 && b >= 0 && a < buttons.size && b < buttons.size) {
                    buttons[a].text = "?"
                    buttons[b].text = "?"
                    buttons[a].background = rounded(Color.WHITE, 18)
                    buttons[b].background = rounded(Color.WHITE, 18)
                }
                first = -1
                second = -1
            }, 650)
        }
    }

    private fun finishGame() {
        running = false
        handler.removeCallbacks(tick)
        score += max(0, 3000 - seconds * 15)
        scoreText.text = "Score $score"
        AlertDialog.Builder(this)
            .setTitle("🎉 BOARD CLEARED!")
            .setMessage("Time: ${formatTime(seconds)}\nMoves: $moves\nFinal score: $score\n\nGreat memory!")
            .setPositiveButton("PLAY AGAIN") { _, _ -> startGame(level, false) }
            .setNegativeButton("HOME") { _, _ -> showHome() }
            .show()
    }

    private fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

    private fun text(value: String, sp: Int, bold: Boolean, color: Int) = TextView(this).apply {
        text = value
        textSize = sp.toFloat()
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun modeButton(title: String, subtitle: String, color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(22), dp(10), dp(22), dp(10))
        background = rounded(color, 20)
        elevation = dp(3).toFloat()
        isClickable = true
        isFocusable = true
        addView(text(title, 17, true, Color.WHITE), LinearLayout.LayoutParams(-1, dp(34)))
        addView(text(subtitle, 12, false, 0xFFEDE8F5.toInt()), LinearLayout.LayoutParams(-1, dp(24)))
    }

    private fun cardButton(value: String) = Button(this).apply {
        text = value
        textSize = 25f
        isAllCaps = false
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        setTextColor(0xFF242229.toInt())
        background = rounded(Color.WHITE, 18)
        elevation = dp(2).toFloat()
    }

    private fun smallButton(value: String) = Button(this).apply {
        text = value
        textSize = 27f
        isAllCaps = false
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        setTextColor(0xFF3E3945.toInt())
        background = rounded(Color.WHITE, 14)
        elevation = dp(2).toFloat()
    }

    private fun statText(value: String) = text(value, 14, true, 0xFF3E3945)

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), 0x14000000)
    }

    private fun lp(width: Int, height: Int, top: Int) = LinearLayout.LayoutParams(width, dp(height)).apply {
        topMargin = dp(top)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
