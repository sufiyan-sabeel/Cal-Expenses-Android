package com.umaiz.calexpenses.ui.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.data.ConversationEntity
import com.umaiz.calexpenses.databinding.ItemConversationBinding
import com.umaiz.calexpenses.util.Formatters

class ConversationAdapter(
    private var items: List<ConversationEntity>,
    private val onClick: (ConversationEntity) -> Unit,
    private val onDelete: (ConversationEntity) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.VH>() {

    class VH(val b: ItemConversationBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<ConversationEntity>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.b.tvTitle.text = c.title.ifBlank { "Untitled chat" }
        holder.b.tvDate.text = Formatters.dateTime(c.updatedAt)
        holder.itemView.setOnClickListener { onClick(c) }
        holder.b.btnDelete.setOnClickListener { onDelete(c) }
    }
}
