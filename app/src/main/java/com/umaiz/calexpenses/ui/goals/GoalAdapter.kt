package com.umaiz.calexpenses.ui.goals

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.GoalEntity
import com.umaiz.calexpenses.databinding.ItemGoalBinding
import com.umaiz.calexpenses.util.Formatters

class GoalAdapter(
    private var items: List<GoalEntity>,
    private val onClick: (GoalEntity) -> Unit,
    private val onAddMoney: (GoalEntity) -> Unit
) : RecyclerView.Adapter<GoalAdapter.VH>() {

    class VH(val b: ItemGoalBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<GoalEntity>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g = items[position]
        val ctx = holder.itemView.context
        val b = holder.b

        b.tvTitle.text = g.title.ifBlank { "Goal" }
        b.pbProgress.progress = (g.progress * 100).toInt()
        b.pbProgress.progressTintList = ColorStateList.valueOf(
            ContextCompat.getColor(ctx, if (g.isComplete) R.color.success else R.color.accent)
        )
        b.tvPercent.text = if (g.isComplete) "Complete" else "${(g.progress * 100).toInt()}%"
        b.tvPercent.setTextColor(
            ContextCompat.getColor(ctx, if (g.isComplete) R.color.success else R.color.accent)
        )
        b.tvSaved.text = "${Formatters.money(g.saved)} of ${Formatters.money(g.target)}"
        b.tvRemaining.text = if (g.isComplete) "Goal reached"
        else "${Formatters.money(g.remaining)} to go"

        if (g.targetDate != null) {
            b.tvDue.visibility = android.view.View.VISIBLE
            b.tvDue.text = "Due ${Formatters.date(g.targetDate)}"
        } else {
            b.tvDue.visibility = android.view.View.GONE
        }

        b.btnAddMoney.setOnClickListener { onAddMoney(g) }
        holder.itemView.setOnClickListener { onClick(g) }
    }
}
