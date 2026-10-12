package com.umaiz.calexpenses.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.umaiz.calexpenses.data.BudgetEntity
import com.umaiz.calexpenses.data.ExpenseEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a print-ready, multi-page financial statement with Android's built-in
 * [PdfDocument] — no third-party PDF dependency, and fully offline.
 *
 * Layout: A4 portrait (595x842 pt), white page, charcoal type, restrained navy accent,
 * banded tables with repeated headers across pages, page numbers and a footer.
 */
object ReportGenerator {

    private const val PAGE_W = 595f
    private const val PAGE_H = 842f
    private const val MARGIN = 48f
    private const val FOOTER_H = 44f

    private const val INK = 0xFF1F2933.toInt()
    private const val MUTED = 0xFF5C6B7A.toInt()
    private const val ACCENT = 0xFF1E3A5F.toInt()
    private const val RULE = 0xFFD5DBE1.toInt()
    private const val BAND = 0xFFF4F6F8.toInt()
    private const val POSITIVE = 0xFF1B7F5A.toInt()
    private const val NEGATIVE = 0xFF9B2C2C.toInt()

    /** Everything the report needs, already filtered by the caller. */
    data class Input(
        val subtitle: String,
        val periodLabel: String,
        val expenses: List<ExpenseEntity>,
        val budgets: List<BudgetEntity>
    )

    fun write(out: OutputStream, input: Input) {
        val doc = PdfDocument()
        try {
            Builder(doc, input).build()
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    // ------------------------------------------------------------------ builder

    private class Builder(private val doc: PdfDocument, private val input: Input) {

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED; textSize = 10.5f
        }
        private val h2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK; textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK; textSize = 10.5f
        }
        private val bodyBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK; textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED; textSize = 10.5f
        }
        private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RULE; strokeWidth = 0.7f
        }
        private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BAND }
        private val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED; textSize = 8.5f
        }
        private val footRight = Paint(footPaint).apply { textAlign = Paint.Align.RIGHT }

        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN
        private var pageNum = 0

        private val right = PAGE_W - MARGIN
        private val bottomLimit = PAGE_H - MARGIN - FOOTER_H
        private val money = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun build() {
            startPage()
            drawTitle()
            drawSummary()
            drawCategoryTable()
            drawBudgetTable()
            drawLedger()
            finishPage()
        }

        // ---------------------------------------------------------- page control

        private fun startPage() {
            pageNum++
            val p = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNum).create()
            )
            page = p
            canvas = p.canvas
            y = MARGIN
        }

        private fun finishPage() {
            val c = canvas ?: return
            val p = page ?: return
            rulePaint.color = RULE
            c.drawLine(MARGIN, PAGE_H - MARGIN - 18f, right, PAGE_H - MARGIN - 18f, rulePaint)
            c.drawText("Cal-Expenses Financial Report", MARGIN, PAGE_H - MARGIN, footPaint)
            c.drawText("Page $pageNum", right, PAGE_H - MARGIN, footRight)
            doc.finishPage(p)
            page = null
            canvas = null
        }

        private fun ensure(space: Float) {
            if (y + space > bottomLimit) {
                finishPage()
                startPage()
            }
        }

        private fun need(h: Float) { ensure(h) }

        // --------------------------------------------------------------- drawing

        private fun text(s: String, x: Float, paint: Paint) {
            canvas?.drawText(s, x, y, paint)
        }

        private fun textRight(s: String, x: Float, paint: Paint) {
            val p = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
            canvas?.drawText(s, x, y, p)
        }

        private fun lineAt(yy: Float) {
            canvas?.drawLine(MARGIN, yy, right, yy, rulePaint)
        }

        private fun band(yTop: Float, h: Float) {
            canvas?.drawRect(MARGIN, yTop, right, yTop + h, bandPaint)
        }

        private fun ellipsize(s: String, paint: Paint, maxW: Float): String {
            if (paint.measureText(s) <= maxW) return s
            var t = s
            while (t.isNotEmpty() && paint.measureText("$t\u2026") > maxW) t = t.dropLast(1)
            return "$t\u2026"
        }

        private fun heading(label: String) {
            need(34f)
            y += 8f
            text(label, MARGIN, h2Paint)
            y += 6f
            lineAt(y)
            y += 16f
        }

        // ------------------------------------------------------------- sections

        private fun drawTitle() {
            text("Cal-Expenses Financial Report", MARGIN, titlePaint)
            y += 18f
            if (input.subtitle.isNotBlank()) {
                text(input.subtitle, MARGIN, subPaint); y += 14f
            }
            text("Period: ${input.periodLabel}", MARGIN, subPaint); y += 13f
            text("Currency: ${Formatters.currencySymbol}", MARGIN, subPaint); y += 13f
            text("Generated: ${money.format(Date())}", MARGIN, subPaint); y += 10f
            lineAt(y); y += 6f
        }

        private fun totals(): Triple<Double, Double, Int> {
            val expense = input.expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val income = input.expenses.filter { it.type == "INCOME" }.sumOf { it.amount }
            return Triple(expense, income, input.expenses.size)
        }

        private fun drawSummary() {
            heading("Summary")
            val (expense, income, count) = totals()
            val net = income - expense
            val avg = if (count > 0) expense / count else 0.0

            row("Total income", Formatters.money(income), valueColor = POSITIVE)
            row("Total expenses", Formatters.money(expense), valueColor = NEGATIVE)
            row("Net balance", Formatters.money(net), bold = true,
                valueColor = if (net >= 0) POSITIVE else NEGATIVE)
            row("Transactions", count.toString())
            row("Average expense", Formatters.money(avg))
        }

        private fun row(
            label: String,
            value: String,
            bold: Boolean = false,
            valueColor: Int? = null
        ) {
            need(20f)
            text(label, MARGIN, if (bold) bodyBold else bodyPaint)
            val vp = Paint(if (bold) bodyBold else bodyPaint).apply {
                textAlign = Paint.Align.RIGHT
                valueColor?.let { color = it }
            }
            canvas?.drawText(value, right, y, vp)
            y += 17f
        }

        private data class Col(val label: String, val x: Float, val rightAlign: Boolean)

        private fun tableHeader(vararg cols: Col) {
            need(24f)
            band(y - 12f, 20f)
            cols.forEach { c ->
                if (c.rightAlign) {
                    val p = Paint(headPaint).apply { textAlign = Paint.Align.RIGHT }
                    canvas?.drawText(c.label, c.x, y, p)
                } else {
                    canvas?.drawText(c.label, c.x, y, headPaint)
                }
            }
            y += 8f
            lineAt(y)
            y += 13f
        }

        private fun drawCategoryTable() {
            val rows = input.expenses.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { e -> e.value.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }
            heading("Spending by category")
            if (rows.isEmpty()) {
                text("No expenses in this period.", MARGIN, mutedPaint); y += 16f; return
            }
            val total = rows.sumOf { it.second }
            val amountX = right - 70f
            tableHeader(
                Col("Category", MARGIN, false),
                Col("Amount", amountX, true),
                Col("Share", right, true)
            )
            rows.forEachIndexed { i, (cat, amt) ->
                need(20f)
                if (i % 2 == 1) band(y - 12f, 18f)
                text(ellipsize(cat, bodyPaint, amountX - MARGIN - 130f), MARGIN, bodyPaint)
                textRight(Formatters.money(amt), amountX, bodyPaint)
                val share = if (total > 0) (amt / total * 100) else 0.0
                textRight(String.format(Locale.US, "%.1f%%", share), right, mutedPaint)
                y += 17f
            }
            y += 2f
            lineAt(y); y += 13f
            text("Total", MARGIN, bodyBold)
            textRight(Formatters.money(total), amountX, bodyBold)
            y += 6f
        }

        private fun drawBudgetTable() {
            if (input.budgets.isEmpty()) return
            heading("Budget vs actual")
            val actualByCat = input.expenses.filter { it.type == "EXPENSE" }
                .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }

            val colBudget = 300f
            val colActual = 420f
            tableHeader(
                Col("Category", MARGIN, false),
                Col("Budget", colBudget, true),
                Col("Actual", colActual, true),
                Col("Remaining", right, true)
            )
            input.budgets.sortedBy { it.category }.forEachIndexed { i, b ->
                need(20f)
                if (i % 2 == 1) band(y - 12f, 18f)
                val actual = actualByCat[b.category] ?: 0.0
                val remaining = b.amount - actual
                text(ellipsize(b.category, bodyPaint, 140f), MARGIN, bodyPaint)
                textRight(Formatters.money(b.amount), colBudget, bodyPaint)
                textRight(Formatters.money(actual), colActual, bodyPaint)
                val rp = Paint(bodyPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    color = if (remaining >= 0) POSITIVE else NEGATIVE
                }
                canvas?.drawText(
                    (if (remaining >= 0) "" else "-") + Formatters.money(Math.abs(remaining)),
                    right, y, rp
                )
                y += 17f
            }
            y += 6f
        }

        private fun drawLedger() {
            heading("Transaction ledger")
            if (input.expenses.isEmpty()) {
                text("No transactions in this period.", MARGIN, mutedPaint); y += 16f; return
            }
            val colCategory = MARGIN + 78f
            val colNote = MARGIN + 190f
            val noteMax = right - 130f - colNote

            fun header() {
                tableHeader(
                    Col("Date", MARGIN, false),
                    Col("Category", colCategory, false),
                    Col("Note", colNote, false),
                    Col("Amount", right, true)
                )
            }
            header()

            input.expenses.sortedBy { it.date }.forEachIndexed { i, e ->
                if (y + 20f > bottomLimit) {
                    finishPage()
                    startPage()
                    header()
                }
                if (i % 2 == 1) band(y - 12f, 18f)
                text(money.format(Date(e.date)), MARGIN, bodyPaint)
                text(ellipsize(e.category, bodyPaint, colNote - colCategory - 12f), colCategory, bodyPaint)
                text(ellipsize(e.note.ifBlank { "-" }, bodyPaint, noteMax), colNote, mutedPaint)
                val income = e.type == "INCOME"
                val ap = Paint(bodyPaint).apply {
                    textAlign = Paint.Align.RIGHT
                    color = if (income) POSITIVE else INK
                }
                canvas?.drawText(
                    (if (income) "+ " else "- ") + Formatters.money(e.amount), right, y, ap
                )
                y += 17f
            }

            y += 2f
            lineAt(y); y += 13f
            val (expense, income, _) = totals()
            text("Totals", MARGIN, bodyBold)
            textRight("+ " + Formatters.money(income), right, bodyBold)
            y += 16f
            textRight("- " + Formatters.money(expense), right, bodyBold)
            y += 16f
            textRight("Net " + Formatters.money(income - expense), right, bodyBold)
            y += 6f
        }
    }
}
