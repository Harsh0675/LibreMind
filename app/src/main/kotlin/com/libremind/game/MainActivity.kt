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
        if (game.currentScreen == Screen.GAME) game.goHome() else super.onBackPressed()
    }
}

private enum class Screen { HOME, GAME }

private data class Mode(val title: String, val subtitle: String, val color: Int, val size: Int, val rush: Boolean)

private class LibreMindView(context: Context) : View(context) {
    val currentScreen: Screen get() = screen
    private val bg = Color.rgb(12, 12, 24)
    private val panel = Color.rgb(24, 23, 43)
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
    private var screen = Screen.HOME
    private var mode = modes[0]
    private var cards = mutableListOf<Int>()
    private var revealed = BooleanArray(0)
    private var matched = BooleanArray(0)
    private var first = -1
    private var moves = 0
    private var matches = 0
    private var score = 0
    private var combo = 0
    private var seconds = 0
    private var running = false
    private var locked = false
    private var pressedIndex = -1
    private var gridLeft = 0f
    private var gridTop = 0f
    private var cell = 0f
    private var gap = 8f
    private var homeButtons = mutableListOf<RectF>()

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
            round(c, r.left, r.top, r.right, r.bottom, 22f, m.color)
            round(c, r.left + 7f, r.top + 7f, r.left + 63f, r.bottom - 7f, 17f, Color.argb(35, 255, 255, 255))
            text(c, if (i == 0) "◆" else if (i == 1) "⚡" else "○", r.left + 35f, r.top + 39f, 23f, white, true, center = true)
            text(c, m.title, r.left + 78f, r.top + 31f, 16f, white, true)
            text(c, m.subtitle, r.left + 78f, r.top + 52f, 12f, Color.rgb(232, 230, 245), false)
            text(c, "›", r.right - 25f, r.top + 39f, 28f, white, false, center = true)
        }
        text(c, "100% OFFLINE   ·   NO ADS   ·   NO ACCOUNT", w / 2f, height - 28f, 10f, Color.rgb(116, 112, 140), true, center = true)
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
        text(c, if (locked) "MATCHING…" else if (combo >= 2) "COMBO ×$combo" else "FIND THE PAIRS", w / 2f, 127f, 11f, if (combo >= 2) cyan else muted, true, center = true)
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
        val radius = if (n == 6) 12f else 15f
        if (open) {
            round(c, r.left, r.top, r.right, r.bottom, radius, cardFront)
            drawIcon(c, cards[index], r.centerX(), r.centerY(), cell * .33f)
        } else {
            round(c, r.left, r.top, r.right, r.bottom, radius, cardBack)
            stroke.color = Color.argb(45, 255, 255, 255)
            stroke.strokeWidth = 1.2f
            c.drawRoundRect(r, radius, radius, stroke)
            drawBackMark(c, r.centerX(), r.centerY(), cell * .23f)
        }
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
        when (type % 6) {
            0 -> c.drawCircle(cx, cy, s * .72f, paint)
            1 -> triangle(c, cx, cy - s * .05f, s * .78f, paint)
            2 -> { c.save(); c.rotate(45f, cx, cy); c.drawRoundRect(RectF(cx-s*.62f, cy-s*.62f, cx+s*.62f, cy+s*.62f), s*.15f, s*.15f, paint); c.restore() }
            3 -> star(c, cx, cy, s, paint)
            4 -> ring(c, cx, cy, s, paint)
            else -> heart(c, cx, cy + s*.08f, s, paint)
        }
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) { val path=Path(); path.moveTo(cx,cy-s); path.lineTo(cx+s,cy+s*.75f); path.lineTo(cx-s,cy+s*.75f); path.close(); c.drawPath(path,p) }
    private fun star(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) { val path=Path(); for(i in 0 until 10){val a=-Math.PI/2+i*Math.PI/5; val r=if(i%2==0)s else s*.42f; val x=cx+cos(a).toFloat()*r; val y=cy+sin(a).toFloat()*r; if(i==0)path.moveTo(x,y) else path.lineTo(x,y)}; path.close(); c.drawPath(path,p) }
    private fun ring(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) { p.style=Paint.Style.STROKE; p.strokeWidth=s*.22f; c.drawCircle(cx,cy,s*.58f,p); p.style=Paint.Style.FILL }
    private fun heart(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) { val path=Path(); path.moveTo(cx,cy+s*.72f); path.cubicTo(cx-s*1.05f,cy+s*.05f,cx-s*.62f,cy-s*.72f,cx,cy-s*.25f); path.cubicTo(cx+s*.62f,cy-s*.72f,cx+s*1.05f,cy+s*.05f,cx,cy+s*.72f); path.close(); c.drawPath(path,p) }
    private fun round(c: Canvas,l:Float,t:Float,r:Float,b:Float,rad:Float,color:Int){paint.style=Paint.Style.FILL;paint.color=color;paint.shader=null;c.drawRoundRect(RectF(l,t,r,b),rad,rad,paint)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean,center:Boolean=false){paint.style=Paint.Style.FILL;paint.color=color;paint.textSize=size;paint.typeface=Typeface.create("sans",if(bold)Typeface.BOLD else Typeface.NORMAL);paint.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,paint)}
    private fun stat(c:Canvas,l:Float,t:Float,r:Float,b:Float,label:String,value:String,color:Int){round(c,l,t,r,b,15f,panel);text(c,label,l+10f,t+16f,8f,muted,true);text(c,value,l+10f,t+34f,14f,color,true)}
    private fun formatTime(s:Int)=String.format("%02d:%02d",s/60,s%60)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN -> { if(screen==Screen.HOME){homePressed=homeButtons.indexOfFirst{it.contains(e.x,e.y)}; invalidate()} else {pressedIndex=cardAt(e.x,e.y);invalidate()}; return true }
            MotionEvent.ACTION_UP -> { val p=if(screen==Screen.HOME)homePressed else pressedIndex; if(screen==Screen.HOME && p>=0 && p<3 && homeButtons[p].contains(e.x,e.y)) startGame(modes[p]); else if(screen==Screen.GAME && p>=0 && cardAt(e.x,e.y)==p) flip(p); homePressed=-1;pressedIndex=-1;invalidate();return true }
            MotionEvent.ACTION_CANCEL -> {homePressed=-1;pressedIndex=-1;invalidate();return true}
        }; return true
    }

    private fun cardAt(x:Float,y:Float):Int { if(cell<=0f)return -1; val n=mode.size; val col=((x-gridLeft)/(cell+gap)).toInt();val row=((y-gridTop)/(cell+gap)).toInt();if(col !in 0 until n||row !in 0 until n)return -1;val lx=(x-gridLeft)-col*(cell+gap);val ly=(y-gridTop)-row*(cell+gap);if(lx>cell||ly>cell)return -1;return row*n+col }
    private fun startGame(m:Mode){mode=m;screen=Screen.GAME;cards=(0 until 18).toList().shuffled().take(m.size*m.size/2).let{it+it}.shuffled().toMutableList();revealed=BooleanArray(cards.size);matched=BooleanArray(cards.size);first=-1;moves=0;matches=0;score=0;combo=0;seconds=0;locked=false;running=true;handler.removeCallbacks(tick);handler.postDelayed(tick,1000);performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);invalidate()}
    private fun flip(i:Int){if(locked||i<0||i>=cards.size||revealed[i]||matched[i])return;revealed[i]=true;performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);if(first<0){first=i;invalidate();return};moves++;val a=first;first=-1;if(cards[a]==cards[i]){matched[a]=true;matched[i]=true;matches++;combo++;score+=100+combo*20;invalidate();if(matches==cards.size/2){running=false;handler.removeCallbacks(tick);handler.postDelayed({showResult("YOU WIN!","Score  $score\nMoves  $moves\nTime  ${formatTime(seconds)}")},350)}}else{combo=0;locked=true;invalidate();handler.postDelayed({revealed[a]=false;revealed[i]=false;locked=false;invalidate()},650)}}
    fun goHome(){handler.removeCallbacks(tick);running=false;screen=Screen.HOME;invalidate()}
    private fun showResult(title:String,msg:String){AlertDialog.Builder(context).setTitle(title).setMessage(msg).setPositiveButton("PLAY AGAIN"){_,_->startGame(mode)}.setNegativeButton("HOME"){_,_->goHome()}.setCancelable(false).show()}
}
