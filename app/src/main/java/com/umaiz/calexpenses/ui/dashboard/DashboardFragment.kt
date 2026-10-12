package com.umaiz.calexpenses.ui.dashboard

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.BudgetEntity
import com.umaiz.calexpenses.data.ExpenseEntity
import com.umaiz.calexpenses.databinding.FragmentDashboardBinding
import com.umaiz.calexpenses.databinding.ItemLegendBinding
import com.umaiz.calexpenses.ui.budgets.BudgetAdapter
import com.umaiz.calexpenses.ui.goals.GoalAdapter
import com.umaiz.calexpenses.ui.transactions.TransactionAdapter
import com.umaiz.calexpenses.ui.transactions.TxRow
import com.umaiz.calexpenses.util.Formatters
import com.umaiz.calexpenses.util.Palette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository

    private lateinit var recentAdapter: TransactionAdapter
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var goalAdapter: GoalAdapter

    private val month = Formatters.currentMonthKey()
    private var latest: List<ExpenseEntity> = emptyList()
    private var currentBudgets: List<BudgetEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentDashboardBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.app_name)

        b.tvGreeting.text = greeting()
        b.tvDate.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        b.tvMonthLabel.text = "BALANCE · " + Formatters.monthLabel(month).uppercase(Locale.getDefault())

        recentAdapter = TransactionAdapter(emptyList()) { }
        b.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        b.rvRecent.adapter = recentAdapter

        budgetAdapter = BudgetAdapter(emptyList(), emptyMap()) { }
        b.rvBudgets.layoutManager = LinearLayoutManager(requireContext())
        b.rvBudgets.adapter = budgetAdapter

        goalAdapter = GoalAdapter(emptyList(), onClick = { }, onAddMoney = { })
        b.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        b.rvGoals.adapter = goalAdapter

        b.btnQuickExpense.setOnClickListener { quickAdd("EXPENSE") }
        b.btnQuickIncome.setOnClickListener { quickAdd("INCOME") }
        b.btnQuickTransfer.setOnClickListener { quickAdd("TRANSFER") }

        repo.expenses.observe(viewLifecycleOwner) { list ->
            latest = list
            renderStatsAndCharts()
            updateBudgets()
        }
        repo.budgetsForMonth(month).observe(viewLifecycleOwner) { budgets ->
            currentBudgets = budgets
            updateBudgets()
        }
        repo.goals.observe(viewLifecycleOwner) { goals ->
            val top = goals.take(2)
            goalAdapter.submit(top)
            b.tvNoGoals.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun greeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    private fun quickAdd(type: String) {
        com.umaiz.calexpenses.ui.transactions.ExpenseSheet(null, type)
            .show(childFragmentManager, "expense")
    }

    private fun renderStatsAndCharts() {
        val monthItems = latest.filter { Formatters.monthKey(it.date) == month }
        // Transfers are deliberately excluded from income/expense totals.
        val expense = monthItems.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val income = monthItems.filter { it.type == "INCOME" }.sumOf { it.amount }
        val net = income - expense

        b.tvBalance.text = Formatters.money(net)
        b.tvBalance.setTextColor(
            ContextCompat.getColor(requireContext(), if (net >= 0) R.color.accent else R.color.danger)
        )
        b.tvIncome.text = Formatters.money(income)
        b.tvExpense.text = Formatters.money(expense)
        b.tvNet.text = Formatters.money(net)

        val months = (5 downTo 0).map { Formatters.shiftMonth(month, -it) }
        b.barChart.setData(months.map { m ->
            val total = latest.filter { it.type == "EXPENSE" && Formatters.monthKey(it.date) == m }
                .sumOf { it.amount }.toFloat()
            Formatters.monthLabel(m).take(3) to total
        })

        val byCat = monthItems.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
            .take(6)

        b.donut.setData(
            byCat.map { it.second.toFloat() to Palette.color(byCat.indexOf(it)) },
            Formatters.money(expense)
        )
        b.legend.removeAllViews()
        byCat.forEachIndexed { i, pair ->
            val row = ItemLegendBinding.inflate(layoutInflater, b.legend, false)
            row.tvLabel.text = pair.first
            row.tvValue.text = Formatters.money(pair.second)
            row.dot.backgroundTintList = ColorStateList.valueOf(Palette.color(i))
            b.legend.addView(row.root)
        }

        val recent = latest.take(5)
        recentAdapter.submit(recent.map { TxRow.Item(it) })
        b.tvNoRecent.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateBudgets() {
        val spentMap = latest
            .filter { it.type == "EXPENSE" && Formatters.monthKey(it.date) == month }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }

        budgetAdapter.submit(currentBudgets, spentMap)
        b.tvNoBudgets.visibility = if (currentBudgets.isEmpty()) View.VISIBLE else View.GONE

        val total = currentBudgets.sumOf { it.amount }
        val spent = currentBudgets.sumOf { spentMap[it.category] ?: 0.0 }
        if (total > 0) {
            val pct = ((spent / total) * 100).toInt().coerceIn(0, 100)
            b.pbBudget.progress = pct
            b.pbBudget.progressTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    when {
                        spent > total -> R.color.danger
                        pct >= 80 -> R.color.warning
                        else -> R.color.accent
                    }
                )
            )
            b.tvBudgetSummary.text = "${Formatters.money(spent)} of ${Formatters.money(total)} used · $pct%"
        } else {
            b.pbBudget.progress = 0
            b.tvBudgetSummary.text = "No budget set for this month"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
