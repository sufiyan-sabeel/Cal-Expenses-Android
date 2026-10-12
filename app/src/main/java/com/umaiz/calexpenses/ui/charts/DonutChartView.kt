package com.umaiz.calexpenses.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Canvas donut chart used for the category breakdown. */
class DonutChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private data class Slice(val value: Float, val color: Int)

    private var slices: List<Slice> = emptyList()
    private var centerLabel: String = ""

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 56f
        strokeCap = Paint.Cap.BUTT
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 44f; textAlign = Paint.Align.CENTER
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA4B2.toInt(); textSize = 28f; textAlign = Paint.Align.CENTER
    }

    fun setData(data: List<Pair<Float, Int>>, center: String) {
        slices = data.map { Slice(it.first, it.second) }
        centerLabel = center
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        val size = Math.min(width, height).toFloat()
        val pad = 56f
        val rect = RectF(
            (width - size) / 2f + pad, (height - size) / 2f + pad,
            (width + size) / 2f - pad, (height + size) / 2f - pad
        )
        if (total <= 0f) {
            arcPaint.color = 0xFF2A2F3A.toInt()
            canvas.drawArc(rect, 0f, 360f, false, arcPaint)
        } else {
            var start = -90f
            slices.forEach { s ->
                val sweep = (s.value / total) * 360f
                arcPaint.color = s.color
                canvas.drawArc(rect, start, sweep, false, arcPaint)
                start += sweep
            }
        }
        val cy = height / 2f
        canvas.drawText(centerLabel, width / 2f, cy + 8f, centerPaint)
        canvas.drawText("total", width / 2f, cy + 46f, subPaint)
    }
}
