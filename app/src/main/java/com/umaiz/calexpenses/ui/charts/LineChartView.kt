package com.umaiz.calexpenses.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Simple canvas line chart for daily / monthly trend. */
class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var points: List<Float> = emptyList()
    private var labels: List<String> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF29B6F6.toInt(); strokeWidth = 6f; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x3329B6F6 }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF80D8FF.toInt() }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA4B2.toInt(); textSize = 28f; textAlign = Paint.Align.CENTER
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A2F3A.toInt(); strokeWidth = 2f
    }

    fun setData(values: List<Float>, labels: List<String>) {
        points = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return
        val padBottom = 48f
        val padTop = 24f
        val padX = 24f
        val w = width.toFloat()
        val h = height.toFloat()
        val chartH = h - padBottom - padTop
        val max = (points.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val step = (w - padX * 2) / (points.size - 1)

        canvas.drawLine(0f, h - padBottom, w, h - padBottom, gridPaint)

        val path = Path()
        val fill = Path()
        points.forEachIndexed { i, v ->
            val x = padX + step * i
            val y = padTop + chartH * (1f - v / max)
            if (i == 0) { path.moveTo(x, y); fill.moveTo(x, h - padBottom); fill.lineTo(x, y) }
            else { path.lineTo(x, y); fill.lineTo(x, y) }
            canvas.drawCircle(x, y, 8f, dotPaint)
            if (labels.size > i) canvas.drawText(labels[i], x, h - padBottom + 36f, labelPaint)
        }
        fill.lineTo(padX + step * (points.size - 1), h - padBottom)
        fill.close()
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}
