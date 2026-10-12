package com.umaiz.calexpenses.ui.budgets

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.BudgetEntity
import com.umaiz.calexpenses.databinding.ItemBudgetBinding
import com.umaiz.calexpenses.util.Formatters

class BudgetAdapter(
    private var items: List<BudgetEntity>,
    private var spent: Map<String, Double>,
    private val onClick: (BudgetEntity) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.VH>() {

    class VH(val b: ItemBudgetBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<BudgetEntity>, spentMap: Map<String, Double>) {
        items = list; spent = spentMap; notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val budget = items[position]
        val ctx = holder.itemView.context
        val b = holder.b
        val used = spent[budget.category] ?: 0.0
        val over = used > budget.amount
        val pct = if (budget.amount <= 0.0) 0 else ((used / budget.amount) * 100).toInt()

        val colorRes = when {
            over -> R.color.danger
            pct >= 80 -> R.color.warning
            else -> R.color.accent
        }

        b.tvCategory.text = budget.category
        b.tvStatus.text = if (over) "Over ${Formatters.money(used - budget.amount)}"
        else "Left ${Formatters.money(budget.amount - used)}"
        b.tvStatus.setTextColor(ContextCompat.getColor(ctx, colorRes))

        b.progress.progress = pct.coerceIn(0, 100)
        b.progress.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, colorRes))

        b.tvSpent.text = "Spent: ${Formatters.money(used)}"
        b.tvLimit.text = "Limit: ${Formatters.money(budget.amount)}"

        holder.itemView.setOnClickListener { onClick(budget) }
    }
}
