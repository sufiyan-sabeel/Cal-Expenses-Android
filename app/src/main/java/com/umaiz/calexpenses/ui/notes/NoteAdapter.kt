package com.umaiz.calexpenses.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.data.NoteEntity
import com.umaiz.calexpenses.databinding.ItemNoteBinding
import com.umaiz.calexpenses.util.Formatters

class NoteAdapter(
    private var items: List<NoteEntity>,
    private val onClick: (NoteEntity) -> Unit
) : RecyclerView.Adapter<NoteAdapter.VH>() {

    class VH(val b: ItemNoteBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<NoteEntity>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]
        holder.b.tvTitle.text = n.title.ifBlank { "Untitled" }
        holder.b.tvSnippet.text = n.text.ifBlank { "(no text)" }
        holder.b.tvDate.text = Formatters.dateTime(n.updatedAt)
        holder.b.ivAudio.visibility = if (n.audioPath.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        holder.itemView.setOnClickListener { onClick(n) }
    }
}
