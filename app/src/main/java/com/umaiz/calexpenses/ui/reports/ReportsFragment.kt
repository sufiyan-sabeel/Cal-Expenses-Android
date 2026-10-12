package com.umaiz.calexpenses.ui.reports

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.ExpenseEntity
import com.umaiz.calexpenses.databinding.FragmentReportsBinding
import com.umaiz.calexpenses.databinding.ItemLegendBinding
import com.umaiz.calexpenses.util.Formatters
import com.umaiz.calexpenses.util.Palette
import com.umaiz.calexpenses.util.ReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class ReportsFragment : Fragment() {

    private var _b: FragmentReportsBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository

    private var month = Formatters.currentMonthKey()
    private var latest: List<ExpenseEntity> = emptyList()
    private var exportAll = false

    private val createPdf = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { exportPdf(it) } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentReportsBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.title_reports)

        b.btnExportPdf.setOnClickListener { chooseExport() }
        b.btnPrev.setOnClickListener { month = Formatters.shiftMonth(month, -1); render() }
        b.btnNext.setOnClickListener { month = Formatters.shiftMonth(month, 1); render() }

        repo.expenses.observe(viewLifecycleOwner) { latest = it; render() }
    }

    private fun render() {
        b.tvMonth.text = Formatters.monthLabel(month)

        val monthExpenses = latest.filter { Formatters.monthKey(it.date) == month }
        val expense = monthExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val income = monthExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }
        val count = monthExpenses.count { it.type == "EXPENSE" }
        val avg = if (count > 0) expense / count else 0.0

        b.tvSummary.text = buildString {
            append("Expenses: ${Formatters.money(expense)}\n")
            append("Income: ${Formatters.money(income)}\n")
            append("Net: ${Formatters.money(income - expense)}\n")
            append("Transactions: ${monthExpenses.size}\n")
            append("Average expense: ${Formatters.money(avg)}")
        }

        // monthly trend (last 6 months, ending at selected month)
        val months = (5 downTo 0).map { Formatters.shiftMonth(month, -it) }
        b.barTrend.setData(months.map { m ->
            val total = latest.filter { it.type == "EXPENSE" && Formatters.monthKey(it.date) == m }
                .sumOf { it.amount }.toFloat()
            Formatters.monthLabel(m).take(3) to total
        })

        // weekly spending within the selected month
        b.barWeekly.setData(weeklyBuckets(monthExpenses))

        // category breakdown
        val byCat = monthExpenses.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
            .take(8)

        b.donut.setData(byCat.map { it.second.toFloat() to Palette.color(byCat.indexOf(it)) }, Formatters.money(expense))
        b.legend.removeAllViews()
        byCat.forEachIndexed { i, pair ->
            val row = ItemLegendBinding.inflate(layoutInflater, b.legend, false)
            row.tvLabel.text = pair.first
            row.tvValue.text = Formatters.money(pair.second)
            row.dot.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.accent)
            row.dot.backgroundTintList = android.content.res.ColorStateList.valueOf(Palette.color(i))
            b.legend.addView(row.root)
        }
    }

    private fun weeklyBuckets(monthExpenses: List<ExpenseEntity>): List<Pair<String, Float>> {
        val cal = Calendar.getInstance()
        val parsed = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).parse(month) ?: return emptyList()
        cal.time = parsed
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val buckets = mutableListOf<Pair<String, Float>>()
        var start = 1
        var week = 1
        while (start <= maxDay) {
            val end = minOf(start + 6, maxDay)
            val total = monthExpenses.filter { it.type == "EXPENSE" }
                .filter {
                    val d = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
                    d in start..end
                }
                .sumOf { it.amount }.toFloat()
            buckets.add("W$week" to total)
            start = end + 1; week++
        }
        return buckets
    }

    private fun chooseExport() {
        val options = arrayOf(
            "This month (${Formatters.monthLabel(month)})",
            "All transactions"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export PDF report")
            .setItems(options) { _, which ->
                exportAll = which == 1
                val name = if (exportAll) "Cal-Expenses-Report-All.pdf"
                else "Cal-Expenses-Report-$month.pdf"
                createPdf.launch(name)
            }
            .show()
    }

    private fun exportPdf(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val all = repo.expensesNow()
                val scoped = if (exportAll) all
                else all.filter { Formatters.monthKey(it.date) == month }
                val budgets = if (exportAll) emptyList()
                else repo.budgetsNow().filter { it.month == month }
                val label = if (exportAll) "All transactions" else Formatters.monthLabel(month)

                val input = ReportGenerator.Input(
                    subtitle = "",
                    periodLabel = label,
                    expenses = scoped.sortedByDescending { it.date },
                    budgets = budgets
                )
                val ctx = requireContext().applicationContext
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { os ->
                        ReportGenerator.write(os, input)
                    } ?: throw IllegalStateException("Could not open destination file")
                }
                Toast.makeText(
                    requireContext(),
                    "Report saved - ${scoped.size} transactions",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
