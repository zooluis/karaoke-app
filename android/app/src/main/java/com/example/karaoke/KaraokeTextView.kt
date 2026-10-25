package com.example.karaoke

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * 卡拉OK歌词视图：当前行大字 + 渐变色逐字高亮，下一行小字预览。
 */
class KaraokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFA79BC8.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val nextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6E6295.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6E6295.toInt()
        textAlign = Paint.Align.CENTER
    }

    private var lines: List<LrcLine> = emptyList()
    private var posMs = 0L
    private var currentIdx = -1

    fun setLrc(list: List<LrcLine>) {
        lines = list
        currentIdx = -1
        invalidate()
    }

    fun setProgressMillis(ms: Long) {
        posMs = ms
        currentIdx = findIndex(ms)
        invalidate()
    }

    fun reset() {
        posMs = 0L
        currentIdx = -1
        invalidate()
    }

    private fun findIndex(ms: Long): Int {
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].time <= ms) idx = i else break
        }
        return idx
    }

    private fun progressOf(line: LrcLine, ms: Long): Float {
        if (line.words.isEmpty()) {
            if (line.end <= line.time) return 0f
            return ((ms - line.time).toFloat() / (line.end - line.time).toFloat()).coerceIn(0f, 1f)
        }
        val ws = line.words
        if (ms <= ws[0].time) return 0f
        for (i in ws.indices) {
            val a = ws[i].time
            val b = if (i + 1 < ws.size) ws[i + 1].time else line.end
            if (ms in a..b && b > a) {
                return (i + (ms - a).toFloat() / (b - a).toFloat()) / ws.size
            }
        }
        return 1f
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        if (lines.isEmpty()) {
            hintPaint.textSize = (h * 0.18f).coerceAtLeast(14f)
            canvas.drawText("尚未载入歌词 — 可选 .lrc 或粘贴", w / 2, h / 2, hintPaint)
            return
        }

        val idx = if (currentIdx in lines.indices) currentIdx else -1
        val next = if (idx + 1 < lines.size) lines[idx + 1] else null

        // 下一句（暗色小字）
        if (next != null) {
            nextPaint.textSize = (h * 0.20f).coerceAtLeast(14f)
            val f = nextPaint.fontMetrics
            canvas.drawText(next.text, w / 2, h * 0.78f - (f.ascent + f.descent) / 2, nextPaint)
        }

        if (idx !in lines.indices) return
        val line = lines[idx]

        // 当前句：字号适配宽度
        var fs = h * 0.44f
        basePaint.textSize = fs
        while (basePaint.measureText(line.text) > w - 24f && fs > 16f) {
            fs -= 1f
            basePaint.textSize = fs
        }
        val textW = basePaint.measureText(line.text)
        val left = w / 2 - textW / 2
        val cy = h * 0.42f
        val fm = basePaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2

        // 先画暗底
        canvas.drawText(line.text, w / 2, baseline, basePaint)

        // 再按进度裁剪画渐变色高亮
        val p = progressOf(line, posMs)
        if (p > 0.005f) {
            highlightPaint.textSize = fs
            highlightPaint.shader = LinearGradient(
                left, 0f, left + textW, 0f,
                intArrayOf(0xFFFF4FD8.toInt(), 0xFFFF8ACB.toInt(), 0xFFFFC94D.toInt()),
                null, Shader.TileMode.CLAMP
            )
            canvas.save()
            canvas.clipRect(left, baseline + fm.ascent, left + textW * p, baseline + fm.descent)
            canvas.drawText(line.text, w / 2, baseline, highlightPaint)
            canvas.restore()
            highlightPaint.shader = null
        }
    }
}
