package com.libremind.game

import android.app.AlertDialog
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var game: LibreMindView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 12, 24)
        window.navigationBarColor = Color.rgb(12, 12, 24)
        game = LibreMindView(this)
        setContentView(game)
    }

    override fun onBackPressed() {
        if (game.screen == Screen.GAME) game.goHome() else super.onBackPressed()
    }
}

private enum class Screen { HOME, GAME }

private data class Mode(val title: String, val subtitle: String, val color: Int, val size: Int, val rush: Boolean)

private class LibreMindView(context: Context) : View(context) {
    private val bg = Color.rgb(12, 12, 24)
    private val panel = Color.rgb(24, 23, 43)
    private val panel2 = Color.rgb(31, 29, 54)
    private val purple = Color.rgb(126, 92, 246)
    private val cyan = Color.rgb(68, 202, 214)
    private val green = Color.rgb(71, 184, 132)
    private val white = Color.rgb(246, 244, 255)
    private val muted = Color.rgb(169, 164, 194)
    private val cardBack = Color.rgb(39, 36, 67)
    private val cardFront = Color.rgb(91, 67, 169)
    private val handler = Handler(Looper.getMainLooper())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val modes = listOf(
        Mode("CLASSIC", "8 pairs · Relaxed", purple, 4, false),
        Mode("TIME RUSH", "8 pairs · 60 seconds", cyan, 4, true),
        Mode("ZEN MODE", "18 pairs · No pressure", green, 6, false)
    )
    private val icons = 0..17
    private var screen = Screen.HOME
    private var mode = modes[0]
    private var cards = mutableListOf<Int>()
    private var revealed = BooleanArray(0)
    private var matched = BooleanArray(0)
    private var first = -1
    private var second = -1
    private var moves = 0
    private var matches = 0
    private var score = 0
    private var combo = 0
    private var seconds = 0
    private var running = false
    private var locked = false
    private var pressedIndex = -1
    private var pressY = 0f
    private var gridLeft = 0f
    private var gridTop = 0f
    private var cell = 0f
    private var gap = 8f
    private var homeButtons = mutableListOf<RectF>()
    private var homePressed = -1
    private var resultShown = false
    private var pulse = 0f

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            seconds++
            invalidate()
            if (mode.rush && seconds >= 60) {
                running = false
                showResult("TIME'S UP", "Score  $score\nMoves  $moves\nTime  01:00")
            } else handler.postDelayed(this, 1000)
        }
    }

    init {
        paint.typeface = Typeface.create("sans", Typeface.NORMAL)
        stroke.style = Paint.Style.STROKE
        isFocusable = true
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        drawBackground(c)
        if (screen == Screen.HOME) drawHome(c) else drawGame(c)
    }

    private fun drawBackground(c: Canvas) {
        val g = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), intArrayOf(bg, Color.rgb(27, 20, 48)), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = g })
        paint.shader = null
        paint.color = Color.argb(20, 150, 110, 255)
        c.drawCircle(width * .82f, height * .12f, width * .42f, paint)
        paint.color = Color.argb(12, 40, 210, 220)
        c.drawCircle(width * .08f, height * .82f, width * .35f, paint)
    }

    private fun drawHome(c: Canvas) {
        val w = width.toFloat()
        val pad = 22f
        var y = 30f
        drawLogo(c, pad, y + 8f, 56f)
        text(c, "LIBREMIND", pad + 70f, y + 28f, 25f, white, true)
        text(c, "MEMORY MATCH", pad + 70f, y + 49f, 11f, muted, true)
        y += 86f

        round(c, pad, y, w - pad, y + 150f, 28f, panel)
        text(c, "MEMORY", pad + 22f, y + 31f, 11f, cyan, true)
        text(c, "Train your mind.", pad + 22f, y + 72f, 28f, white, true)
        text(c, "Match pairs. Build combos. Beat your best.", pad + 22f, y + 101f, 13f, muted, false)
        drawMiniCards(c, w - 116f, y + 30f)
        y += 174f

        text(c, "CHOOSE A MODE", pad, y + 17f, 12f, muted, true)
        y += 34f
        homeButtons.clear()
        modes.forEachIndexed { i, m ->
            val top = y + i * 86f
            val r = RectF(pad, top, w - pad, top + 72f)
            homeButtons.add(r)
            val color = m.color
            round(c, r.left, r.top, r.right, r.bottom, 22f, Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)))
            round(c, r.left + 7f, r.top + 7f, r.left + 63f, r.bottom - 7f, 17f, Color.argb(35, 255, 255, 255))
            text(c, if (i == 0) "◆" else if (i == 1) "⚡" else "○", r.left + 35f, r.top + 39f, 23f, white, true, center = true)
            text(c, m.title, r.left + 78f, r.top + 31f, 16f, white, true)
            text(c, m.subtitle, r.left + 78f, r.top + 52f, 12f, Color.rgb(232, 230, 245), false)
            text(c, "›", r.right - 25f, r.top + 39f, 28f, white, false, center = true)
        }
        y += 86f * 3f + 4f
        text(c, "100% OFFLINE   ·   NO ADS   ·   NO ACCOUNT", w / 2f, min(height - 28f, y + 14f), 10f, Color.rgb(116, 112, 140), true, center = true)
    }

    private fun drawGame(c: Canvas) {
        val w = width.toFloat()
        text(c, "‹", 26f, 43f, 34f, white, false, center = true)
        text(c, mode.title, w / 2f, 34f, 16f, white, true, center = true)
        text(c, "${matches}/${cards.size / 2}", w - 30f, 35f, 12f, muted, true, center = true)

        val statY = 57f
        stat(c, 18f, statY, w * .31f - 5f, statY + 42f, "TIME", formatTime(seconds), if (mode.rush) cyan else purple)
        stat(c, w * .345f, statY, w * .655f, statY + 42f, "MOVES", moves.toString(), white)
        stat(c, w * .69f, statY, w - 18f, statY + 42f, "SCORE", score.toString(), green)

        val hint = if (locked) "MATCHING…" else if (combo >= 2) "COMBO ×$combo" else "FIND THE PAIRS"
        text(c, hint, w / 2f, 127f, 11f, if (combo >= 2) cyan else muted, true, center = true)

        val n = mode.size
        val availableW = w - 30f
        val availableH = height - 155f
        val board = min(availableW, availableH)
        gap = if (n == 6) 5f else 7f
        cell = (board - gap * (n - 1)) / n
        gridLeft = (w - board) / 2f
        gridTop = 145f + (availableH - board) / 2f

        for (i in cards.indices) drawCard(c, i, n)
    }

    private fun drawCard(c: Canvas, index: Int, n: Int) {
        val col = index % n
        val row = index / n
        val x = gridLeft + col * (cell + gap)
        val y = gridTop + row * (cell + gap)
        val r = RectF(x, y, x + cell, y + cell)
        val open = revealed[index] || matched[index]
        val isPressed = pressedIndex == index
        val scale = if (isPressed) .94f else 1f
        val cx = r.centerX(); val cy = r.centerY()
        c.save(); c.scale(scale, scale, cx, cy)
        if (open) {
            round(c, r.left, r.top, r.right, r.bottom, if (n == 6) 12f else 15f, cardFront)
            drawIcon(c, cards[index], cx, cy, cell * .33f)
        } else {
            round(c, r.left, r.top, r.right, r.bottom, if (n == 6) 12f else 15f, cardBack)
            stroke.color = Color.argb(45, 255, 255, 255)
            stroke.strokeWidth = 1.2f
            c.drawRoundRect(r, if (n == 6) 12f else 15f, if (n == 6) 12f else 15f, stroke)
            drawBackMark(c, cx, cy, cell * .23f)
        }
        c.restore()
    }

    private fun drawLogo(c: Canvas, x: Float, y: Float, s: Float) {
        round(c, x, y, x + s, y + s, 19f, purple)
        paint.color = white
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        c.drawCircle(x + s / 2f, y + s / 2f, s * .27f, paint)
        c.drawLine(x + s * .28f, y + s * .58f, x + s * .72f, y + s * .58f, paint)
        paint.style = Paint.Style.FILL
        c.drawCircle(x + s * .38f, y + s * .42f, 3f, paint)
        c.drawCircle(x + s * .62f, y + s * .42f, 3f, paint)
    }

    private fun drawMiniCards(c: Canvas, x: Float, y: Float) {
        round(c, x + 22f, y + 18f, x + 62f, y + 72f, 11f, purple)
        round(c, x, y + 2f, x + 40f, y + 56f, 11f, cyan)
        text(c, "?", x + 20f, y + 34f, 20f, white, true, center = true)
        text(c, "★", x + 42f, y + 47f, 19f, white, true, center = true)
    }

    private fun drawBackMark(c: Canvas, cx: Float, cy: Float, s: Float) {
        paint.color = Color.argb(95, 126, 92, 246)
        c.drawCircle(cx, cy, s, paint)
        paint.color = Color.argb(150, 246, 244, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(2f, s * .10f)
        c.drawCircle(cx, cy, s * .48f, paint)
        c.drawLine(cx - s * .22f, cy, cx + s * .22f, cy, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawIcon(c: Canvas, type: Int, cx: Float, cy: Float, s: Float) {
        paint.color = when (type % 6) { 0 -> Color.rgb(255, 196, 88); 1 -> Color.rgb(92, 219, 225); 2 -> Color.rgb(255, 121, 153); 3 -> Color.rgb(142, 226, 151); 4 -> Color.rgb(205, 163, 255); else -> Color.rgb(255, 151, 89) }
        when (type) {
            0 -> c.drawCircle(cx, cy, s * .72f, paint)
            1 -> triangle(c, cx, cy - s * .05f, s * .78f, paint)
            2 -> { c.save(); c.rotate(45f, cx, cy); c.drawRoundRect(RectF(cx-s*.62f, cy-s*.62f, cx+s*.62f, cy+s*.62f), s*.15f, s*.15f, paint); c.restore() }
            3 -> star(c, cx, cy, s, paint)
            4 -> ring(c, cx, cy, s, paint)
            5 -> heart(c, cx, cy + s*.08f, s, paint)
            6 -> c.drawRect(cx-s*.65f, cy-s*.65f, cx+s*.65f, cy+s*.65f, paint)
            7 -> hex(c, cx, cy, s*.75f, paint)
            8 -> { c.drawCircle(cx, cy, s*.7f, paint); paint.color = cardFront; c.drawCircle(cx+s*.23f, cy-s*.18f, s*.17f, paint) }
            9 -> { c.drawCircle(cx, cy, s*.7f, paint); paint.color = cardFront; c.drawRect(cx-s*.08f, cy-s*.75f, cx+s*.08f, cy+s*.75f, paint) }
            10 -> cross(c, cx, cy, s, paint)
            11 -> { c.drawCircle(cx, cy, s*.72f, paint); paint.color = cardFront; c.drawCircle(cx, cy, s*.27f, paint) }
            12 -> { triangle(c, cx, cy+s*.05f, s*.82f, paint); paint.color = cardFront; triangle(c, cx, cy-s*.22f, s*.3f, paint) }
            13 -> { c.drawRoundRect(RectF(cx-s*.72f, cy-s*.45f, cx+s*.72f, cy+s*.45f), s*.25f, s*.25f, paint) }
            14 -> star(c, cx, cy, s*.8f, paint)
            15 -> { c.drawCircle(cx, cy, s*.68f, paint); paint.color = cardFront; c.drawCircle(cx-s*.2f, cy-s*.2f, s*.1f, paint); c.drawCircle(cx+s*.2f, cy+s*.2f, s*.1f, paint) }
            16 -> { hex(c, cx, cy, s*.75f, paint); paint.color = cardFront; c.drawCircle(cx, cy, s*.22f, paint) }
            else -> { c.drawRect(cx-s*.62f, cy-s*.62f, cx+s*.62f, cy+s*.62f, paint); paint.color = cardFront; c.drawCircle(cx, cy, s*.22f, paint) }
        }
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        val path = Path(); path.moveTo(cx, cy-s); path.lineTo(cx+s*.9f, cy+s*.72f); path.lineTo(cx-s*.9f, cy+s*.72f); path.close(); c.drawPath(path, p)
    }
    private fun star(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        val path = Path(); for (i in 0 until 10) { val a = -Math.PI/2 + i*Math.PI/5; val rr = if (i%2==0) s else s*.43f; val x=cx+cos(a).toFloat()*rr; val y=cy+sin(a).toFloat()*rr; if(i==0) path.moveTo(x,y) else path.lineTo(x,y) }; path.close(); c.drawPath(path,p)
    }
    private fun ring(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) { p.style=Paint.Style.STROKE; p.strokeWidth=s*.25f; c.drawCircle(cx,cy,s*.55f,p); p.style=Paint.Style.FILL }
    private fun heart(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) { val path=Path(); path.moveTo(cx,cy+s*.75f); path.cubicTo(cx-s,cy+s*.05f,cx-s*.7f,cy-s*.65f,cx,cy-s*.15f); path.cubicTo(cx+s*.7f,cy-s*.65f,cx+s,cy+s*.05f,cx,cy+s*.75f); path.close(); c.drawPath(path,p) }
    private fun hex(c: Canvas,cx:Float,cy:Float,s:Float,p:Paint){val path=Path();for(i in 0..5){val a=i*Math.PI/3;val x=cx+cos(a).toFloat()*s;val y=cy+sin(a).toFloat()*s;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};path.close();c.drawPath(path,p)}
    private fun cross(c:Canvas,cx:Float,cy:Float,s:Float,p:Paint){c.drawRoundRect(RectF(cx-s*.18f,cy-s*.75f,cx+s*.18f,cy+s*.75f),s*.15f,s*.15f,p);c.drawRoundRect(RectF(cx-s*.75f,cy-s*.18f,cx+s*.75f,cy+s*.18f),s*.15f,s*.15f,p)}

    private fun stat(c: Canvas, l: Float, t: Float, r: Float, b: Float, label: String, value: String, color: Int) {
        round(c, l, t, r, b, 14f, panel)
        text(c, label, l + 10f, t + 15f, 8f, muted, true)
        text(c, value, l + 10f, t + 33f, 13f, color, true)
    }

    private fun round(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = color; c.drawRoundRect(RectF(l,t,r,b),radius,radius,paint)
    }
    private fun text(c: Canvas, s: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean, center: Boolean = false) {
        paint.shader=null; paint.style=Paint.Style.FILL; paint.color=color; paint.textSize=size; paint.typeface=Typeface.create("sans",if(bold)Typeface.BOLD else Typeface.NORMAL); paint.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT; c.drawText(s,x,baseline,paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressY = e.y
                if (screen == Screen.HOME) {
                    homePressed = homeButtons.indexOfFirst { it.contains(e.x,e.y) }
                    if (homePressed >= 0) { invalidate(); return true }
                } else {
                    if (e.x < 58f && e.y < 65f) { goHome(); return true }
                    pressedIndex = cardAt(e.x,e.y)
                    if (pressedIndex >= 0) invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (screen == Screen.HOME) {
                    val p=homePressed; homePressed=-1; invalidate(); if(p>=0 && p< modes.size && homeButtons.getOrNull(p)?.contains(e.x,e.y)==true) startGame(modes[p]); return true
                }
                val p=pressedIndex; pressedIndex=-1; invalidate(); if(p>=0 && cardAt(e.x,e.y)==p && kotlin.math.abs(e.y-pressY)<40f) flip(p); return true
            }
            MotionEvent.ACTION_CANCEL -> { homePressed=-1; pressedIndex=-1; invalidate(); return true }
        }
        return true
    }

    private fun cardAt(x: Float, y: Float): Int {
        if (cards.isEmpty()) return -1
        val n=mode.size; val col=((x-gridLeft)/(cell+gap)).toInt(); val row=((y-gridTop)/(cell+gap)).toInt(); if(col !in 0 until n || row !in 0 until n) return -1
        val lx=x-(gridLeft+col*(cell+gap)); val ly=y-(gridTop+row*(cell+gap)); if(lx>cell || ly>cell) return -1
        return row*n+col
    }

    private fun startGame(m: Mode) {
        mode=m; val pairCount=m.size*m.size/2; val selected=icons.shuffled(Random(System.nanoTime())).take(pairCount); cards=(selected+selected).shuffled(Random(System.nanoTime())).toMutableList(); revealed=BooleanArray(cards.size); matched=BooleanArray(cards.size); first=-1; second=-1; moves=0; matches=0; score=0; combo=0; seconds=0; locked=false; resultShown=false; screen=Screen.GAME; running=true; handler.removeCallbacks(tick); handler.postDelayed(tick,1000); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate()
    }

    private fun flip(i:Int){
        if(!running || locked || i !in cards.indices || revealed[i] || matched[i]) return
        revealed[i]=true; performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); animateCard(i)
        if(first==-1){first=i;invalidate();return}
        second=i; moves++; locked=true; if(cards[first]==cards[second]){
            matched[first]=true; matched[second]=true; matches++; combo++; score += 100 + combo*25; locked=false; first=-1;second=-1
            if(matches==cards.size/2){handler.postDelayed({finishGame()},450)}
        }else{
            combo=0; val a=first;val b=second; handler.postDelayed({revealed[a]=false;revealed[b]=false;first=-1;second=-1;locked=false;invalidate()},700)
        }
        invalidate()
    }

    private fun animateCard(i:Int){ val a=ValueAnimator.ofFloat(.82f,1f);a.duration=170;a.addUpdateListener{pulse=it.animatedValue as Float;invalidate()};a.start() }

    private fun finishGame(){if(!running||resultShown)return;running=false;handler.removeCallbacks(tick);score+=maxOf(0,3000-seconds*15);showResult("BRILLIANT!","You matched every pair.\n\nScore  $score\nMoves  $moves\nTime  ${formatTime(seconds)}")}
    private fun showResult(title:String,msg:String){if(resultShown)return;resultShown=true;AlertDialog.Builder(context).setTitle(title).setMessage(msg).setPositiveButton("PLAY AGAIN"){_,_->startGame(mode)}.setNegativeButton("HOME"){_,_->goHome()}.setOnCancelListener{goHome()}.show()}
    fun goHome(){running=false;handler.removeCallbacks(tick);locked=false;first=-1;second=-1;screen=Screen.HOME;resultShown=false;invalidate()}
    private fun formatTime(s:Int)="%02d:%02d".format(s/60,s%60)
}
