package com.umaiz.calexpenses.ui.budgets

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
import com.umaiz.calexpenses.databinding.FragmentBudgetsBinding
import com.umaiz.calexpenses.util.Formatters

class BudgetsFragment : Fragment() {

    private var _b: FragmentBudgetsBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository
    private lateinit var adapter: BudgetAdapter

    private var month = Formatters.currentMonthKey()
    private var latest: List<ExpenseEntity> = emptyList()
    private var currentBudgets: List<BudgetEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentBudgetsBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.nav_budgets)

        adapter = BudgetAdapter(emptyList(), emptyMap()) { budget ->
            BudgetSheet(month, budget).show(childFragmentManager, "budget")
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        b.fabAdd.setOnClickListener {
            BudgetSheet(month, null).show(childFragmentManager, "budget")
        }
        b.btnPrev.setOnClickListener { month = Formatters.shiftMonth(month, -1); reload() }
        b.btnNext.setOnClickListener { month = Formatters.shiftMonth(month, 1); reload() }

        repo.expenses.observe(viewLifecycleOwner) { latest = it; update() }
        reload()
    }

    private fun reload() {
        b.tvMonth.text = Formatters.monthLabel(month)
        repo.budgetsForMonth(month).observe(viewLifecycleOwner) { budgets ->
            currentBudgets = budgets
            update()
        }
    }

    private fun update() {
        val spentMap = latest
            .filter { it.type == "EXPENSE" && Formatters.monthKey(it.date) == month }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }

        adapter.submit(currentBudgets, spentMap)
        b.tvEmpty.visibility = if (currentBudgets.isEmpty()) View.VISIBLE else View.GONE

        val total = currentBudgets.sumOf { it.amount }
        val spent = currentBudgets.sumOf { spentMap[it.category] ?: 0.0 }
        b.tvTotalSpent.text = Formatters.money(spent)
        b.tvTotalOf.text = "of ${Formatters.money(total)}"
        b.tvTotalRemaining.text = if (spent > total)
            "Over by ${Formatters.money(spent - total)}" else "${Formatters.money(total - spent)} remaining"

        val pct = if (total > 0) ((spent / total) * 100).toInt().coerceIn(0, 100) else 0
        b.tvTotalPct.text = "$pct% used"
        b.pbTotal.progress = pct
        b.pbTotal.progressTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                when {
                    spent > total && total > 0 -> R.color.danger
                    pct >= 80 -> R.color.warning
                    else -> R.color.accent
                }
            )
        )
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
