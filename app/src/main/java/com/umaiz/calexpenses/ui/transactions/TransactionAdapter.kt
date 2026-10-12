package com.umaiz.calexpenses.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.ExpenseEntity
import com.umaiz.calexpenses.databinding.ItemTransactionBinding
import com.umaiz.calexpenses.databinding.ItemTxHeaderBinding
import com.umaiz.calexpenses.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private var rows: List<TxRow>,
    private val onClick: (ExpenseEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class HeaderVH(val b: ItemTxHeaderBinding) : RecyclerView.ViewHolder(b.root)
    class ItemVH(val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<TxRow>) {
        rows = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) =
        if (rows[position] is TxRow.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderVH(ItemTxHeaderBinding.inflate(inf, parent, false))
        else ItemVH(ItemTransactionBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is TxRow.Header -> (holder as HeaderVH).b.tvHeader.text = row.label
            is TxRow.Item -> bindItem(holder as ItemVH, row.expense)
        }
    }

    private fun bindItem(holder: ItemVH, e: ExpenseEntity) {
        val ctx = holder.itemView.context
        val b = holder.b
        b.tvTitle.text = e.note.ifBlank { e.category }
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(e.date))
        b.tvSub.text = "${e.category}  ·  $time"

        val transfer = e.type == "TRANSFER"
        val income = e.type == "INCOME"
        val sign = when {
            transfer -> ""
            income -> "+ "
            else -> "- "
        }
        b.tvAmount.text = sign + Formatters.money(e.amount)

        val colorRes = when {
            transfer -> R.color.text_secondary
            income -> R.color.success
            else -> R.color.danger
        }
        b.tvAmount.setTextColor(ContextCompat.getColor(ctx, colorRes))
        b.ivIcon.setImageResource(if (income) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up)
        b.ivIcon.imageTintList = ContextCompat.getColorStateList(ctx, colorRes)
        holder.itemView.setOnClickListener { onClick(e) }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }
}
