package com.umaiz.calexpenses.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Lightweight canvas bar chart — no third-party dependency. */
class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private data class Bar(val label: String, val value: Float)

    private var bars: List<Bar> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF29B6F6.toInt() }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA4B2.toInt(); textSize = 30f; textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 28f; textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A2F3A.toInt(); strokeWidth = 2f
    }

    fun setData(data: List<Pair<String, Float>>) {
        bars = data.map { Bar(it.first, it.second) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return
        val padBottom = 56f
        val padTop = 44f
        val w = width.toFloat()
        val h = height.toFloat()
        val chartH = h - padBottom - padTop
        val max = (bars.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(1f)
        val slot = w / bars.size
        val barW = slot * 0.5f

        canvas.drawLine(0f, h - padBottom, w, h - padBottom, gridPaint)

        bars.forEachIndexed { i, b ->
            val cx = slot * i + slot / 2f
            val bh = (b.value / max) * chartH
            val top = h - padBottom - bh
            val rect = RectF(cx - barW / 2f, top, cx + barW / 2f, h - padBottom)
            canvas.drawRoundRect(rect, 12f, 12f, barPaint)
            canvas.drawText(b.label, cx, h - padBottom + 38f, labelPaint)
            if (b.value > 0f) canvas.drawText(shortVal(b.value), cx, top - 14f, valuePaint)
        }
    }

    private fun shortVal(v: Float): String = when {
        v >= 100000 -> String.format(java.util.Locale.US, "%.1fL", v / 100000)
        v >= 1000 -> String.format(java.util.Locale.US, "%.1fk", v / 1000)
        else -> String.format(java.util.Locale.US, "%.0f", v)
    }
}
