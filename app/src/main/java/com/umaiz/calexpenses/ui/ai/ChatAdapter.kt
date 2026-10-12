package com.umaiz.calexpenses.ui.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umaiz.calexpenses.data.MessageEntity
import com.umaiz.calexpenses.databinding.ItemMessageAiBinding
import com.umaiz.calexpenses.databinding.ItemMessageUserBinding

class ChatAdapter(private var items: List<MessageEntity>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class UserVH(val b: ItemMessageUserBinding) : RecyclerView.ViewHolder(b.root)
    class AiVH(val b: ItemMessageAiBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<MessageEntity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        if (items[position].role == "user") TYPE_USER else TYPE_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) UserVH(ItemMessageUserBinding.inflate(inf, parent, false))
        else AiVH(ItemMessageAiBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val text = items[position].text
        when (holder) {
            is UserVH -> holder.b.tvMessage.text = text
            is AiVH -> holder.b.tvMessage.text = text
        }
    }

    companion object {
        const val TYPE_USER = 1
        const val TYPE_AI = 2
    }
}
