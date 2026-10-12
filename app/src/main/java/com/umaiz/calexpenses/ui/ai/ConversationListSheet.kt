package com.umaiz.calexpenses.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.ConversationEntity
import com.umaiz.calexpenses.databinding.SheetConversationsBinding
import kotlinx.coroutines.launch

class ConversationListSheet(private val onOpen: (ConversationEntity) -> Unit) :
    BottomSheetDialogFragment() {

    private var _b: SheetConversationsBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository
    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetConversationsBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ConversationAdapter(
            emptyList(),
            onClick = { c -> onOpen(c); dismiss() },
            onDelete = { c ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete this conversation?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        lifecycleScope.launch { repo.deleteConversation(c) }
                    }.show()
            }
        )
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        repo.conversations.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
