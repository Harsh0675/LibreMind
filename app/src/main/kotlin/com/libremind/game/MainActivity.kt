package com.libremind.game

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val symbols = listOf("🍎","🚀","🌙","⭐","🎵","🐼","🌈","🔥","🍀","⚡","🎮","🦊","🌻","🍕","🐳","💎","🧩","🎯")
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 40, 48, 40)
            setBackgroundColor(0xFFF8F7FC.toInt())
        }
        val title = text("LIBREMIND", 34, true)
        val sub = text("Train your memory. Beat your best.", 17, false)
        val play = button("▶  PLAY CLASSIC")
        val rush = button("⚡  TIME RUSH")
        val zen = button("☁  ZEN MODE")
        val info = text("Offline • No account • No ads • Just play", 14, false)
        root.addView(title)
        root.addView(sub, lp(0,24))
        root.addView(play, lp(0,32))
        root.addView(rush, lp(0,12))
        root.addView(zen, lp(0,12))
        root.addView(info, lp(0,30))
        play.setOnClickListener { startGame(4, false) }
        rush.setOnClickListener { startGame(4, true) }
        zen.setOnClickListener { startGame(6, false) }
        setContentView(root)
    }

    private fun startGame(size: Int, timeRush: Boolean) {
        level = size
        val pairs = (size * size) / 2
        cards = (symbols.shuffled().take(pairs) * 2).shuffled().toMutableList()
        first = -1; second = -1; moves = 0; matches = 0; combo = 0; score = 0; seconds = 0
        running = true
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 1000)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 12)
            setBackgroundColor(0xFFF8F7FC.toInt())
        }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = button("‹")
        timerText = text("⏱ 00:00", 15, true)
        movesText = text("Moves 0", 15, true)
        scoreText = text("Score 0", 15, true)
        bar.addView(back, LinearLayout.LayoutParams(52, 52))
        bar.addView(timerText, LinearLayout.LayoutParams(0,52,1f))
        bar.addView(movesText, LinearLayout.LayoutParams(0,52,1f))
        bar.addView(scoreText, LinearLayout.LayoutParams(0,52,1f))
        root.addView(bar)
        back.setOnClickListener { running=false; handler.removeCallbacks(tick); showHome() }

        val hint = text(if(timeRush) "⚡ Match pairs before time runs out!" else "🧠 Find every matching pair", 14, false)
        root.addView(hint, lp(0,8))
        grid = GridLayout(this).apply { columnCount = size; rowCount = size }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        buttons.clear()
        cards.indices.forEach { i ->
            val b = button("?", true)
            b.setOnClickListener { flip(i) }
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = 0; height = 0
                columnSpec = GridLayout.spec(i % size, 1, 1f)
                rowSpec = GridLayout.spec(i / size, 1, 1f)
                setMargins(5,5,5,5)
            })
            buttons.add(b)
        }
    }

    private fun flip(index: Int) {
        if (!running || index == first || buttons[index].isEnabled.not()) return
        if (first != -1 && second != -1) return
        buttons[index].text = cards[index]
        buttons[index].setBackgroundColor(0xFFE9DDFB.toInt())
        if (first == -1) { first = index; return }
        second = index
        moves++
        movesText.text = "Moves $moves"
        if (cards[first] == cards[second]) {
            buttons[first].isEnabled = false
            buttons[second].isEnabled = false
            matches++
            combo++
            score += 100 + combo * 20
            scoreText.text = "Score $score"
            first = -1; second = -1
            if (matches == cards.size / 2) finishGame()
        } else {
            combo = 0
            handler.postDelayed({
                buttons[first].text = "?"
                buttons[second].text = "?"
                buttons[first].setBackgroundColor(0xFFFFFFFF.toInt())
                buttons[second].setBackgroundColor(0xFFFFFFFF.toInt())
                first = -1; second = -1
            }, 650)
        }
    }

    private fun finishGame() {
        running = false
        handler.removeCallbacks(tick)
        score += max(0, 3000 - seconds * 15)
        scoreText.text = "Score $score"
        AlertDialogBuilder().show(
            "🎉 BOARD CLEARED!",
            "Time: ${formatTime(seconds)}\nMoves: $moves\nFinal score: $score\n\nGreat memory!"
        )
    }

    private fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

    private fun text(s: String, sp: Int, bold: Boolean) = TextView(this).apply {
        text = s; textSize = sp.toFloat(); gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(s: String, compact: Boolean=false) = Button(this).apply {
        text = s; textSize = if(compact) 24f else 16f; isAllCaps = false
        minHeight = 0; minimumHeight = 0
        setPadding(4,4,4,4)
        setBackgroundColor(0xFFFFFFFF.toInt())
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(if(w==0) -1 else w, h).apply {
        topMargin = h
    }

    private inner class AlertDialogBuilder {
        fun show(title: String, msg: String) {
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(title).setMessage(msg)
                .setPositiveButton("PLAY AGAIN") { _, _ -> startGame(level, false) }
                .setNegativeButton("HOME") { _, _ -> showHome() }.show()
        }
    }
}
