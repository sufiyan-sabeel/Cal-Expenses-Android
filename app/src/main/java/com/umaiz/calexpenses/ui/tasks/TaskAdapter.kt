package com.umaiz.calexpenses.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.data.TaskEntity
import com.umaiz.calexpenses.databinding.ItemTaskBinding
import com.umaiz.calexpenses.util.Checklist
import com.umaiz.calexpenses.util.Formatters

class TaskAdapter(
    private var items: List<TaskEntity>,
    private val onToggle: (TaskEntity, Boolean) -> Unit,
    private val onClick: (TaskEntity) -> Unit,
    private val onDelete: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    class VH(val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<TaskEntity>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.b.cbDone.setOnCheckedChangeListener(null)
        holder.b.cbDone.isChecked = t.done
        holder.b.tvTitle.text = t.title.ifBlank { "Untitled task" }
        holder.b.tvTitle.alpha = if (t.done) 0.5f else 1f

        val parts = mutableListOf<String>()
        t.dueDate?.let { parts.add("Due ${Formatters.date(it)}") }
        val checks = Checklist.fromJson(t.checklistJson)
        if (checks.isNotEmpty()) parts.add("${checks.count { it.done }}/${checks.size} steps")
        holder.b.tvMeta.text = parts.joinToString("  ·  ")
        holder.b.tvMeta.visibility = if (parts.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

        holder.b.cbDone.setOnCheckedChangeListener { _, checked -> onToggle(t, checked) }
        holder.itemView.setOnClickListener { onClick(t) }
        holder.b.btnDelete.setOnClickListener { onDelete(t) }
    }
}
