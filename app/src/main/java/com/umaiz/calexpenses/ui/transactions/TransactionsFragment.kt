package com.umaiz.calexpenses.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.ExpenseEntity
import com.umaiz.calexpenses.databinding.FragmentTransactionsBinding
import com.umaiz.calexpenses.util.Formatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionsFragment : Fragment() {

    private var _b: FragmentTransactionsBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository
    private lateinit var adapter: TransactionAdapter
    private var latest: List<ExpenseEntity> = emptyList()
    private var filter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentTransactionsBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.nav_transactions)

        adapter = TransactionAdapter(emptyList()) { e ->
            ExpenseSheet(e).show(childFragmentManager, "expense")
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter
        b.fabAdd.setOnClickListener { ExpenseSheet(null).show(childFragmentManager, "expense") }
        b.etSearch.doAfterTextChanged { render() }

        b.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = when (checkedIds.firstOrNull()) {
                R.id.chipExpenses -> "EXPENSE"
                R.id.chipIncome -> "INCOME"
                R.id.chipTransfers -> "TRANSFER"
                else -> "ALL"
            }
            render()
        }

        repo.expenses.observe(viewLifecycleOwner) { latest = it; render() }
    }

    private fun render() {
        val q = b.etSearch.text?.toString()?.trim().orEmpty()
        var list = latest
        if (filter != "ALL") list = list.filter { it.type == filter }
        if (q.isNotBlank()) {
            list = list.filter {
                it.note.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
            }
        }

        b.tvTotalIncome.text = Formatters.money(list.filter { it.type == "INCOME" }.sumOf { it.amount })
        b.tvTotalExpense.text = Formatters.money(list.filter { it.type == "EXPENSE" }.sumOf { it.amount })

        val rows = buildRows(list)
        adapter.submit(rows)
        b.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun buildRows(list: List<ExpenseEntity>): List<TxRow> {
        val out = mutableListOf<TxRow>()
        val todayKey = dayKey(System.currentTimeMillis())
        val yesterdayKey = dayKey(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        )
        var currentHeader: String? = null
        list.sortedByDescending { it.date }.forEach { e ->
            val key = dayKey(e.date)
            if (key != currentHeader) {
                currentHeader = key
                val label = when (key) {
                    todayKey -> "TODAY"
                    yesterdayKey -> "YESTERDAY"
                    else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(Date(e.date)).uppercase(Locale.getDefault())
                }
                out.add(TxRow.Header(label))
            }
            out.add(TxRow.Item(e))
        }
        return out
    }

    private fun dayKey(millis: Long): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(millis))

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
