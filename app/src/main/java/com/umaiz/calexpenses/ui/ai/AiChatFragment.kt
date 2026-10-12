package com.umaiz.calexpenses.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.MessageEntity
import com.umaiz.calexpenses.databinding.FragmentAiChatBinding
import com.umaiz.calexpenses.util.ChatMessage
import com.umaiz.calexpenses.util.Formatters
import com.umaiz.calexpenses.util.OpenRouterClient
import com.umaiz.calexpenses.util.SecurePrefs
import kotlinx.coroutines.launch

class AiChatFragment : Fragment() {

    private var _b: FragmentAiChatBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository

    private lateinit var adapter: ChatAdapter
    private var currentConversationId: Long? = null
    private var sending = false
    private var source: LiveData<List<MessageEntity>>? = null

    private val messageObserver = Observer<List<MessageEntity>> { list ->
        adapter.submit(list)
        updateEmptyState()
        if (list.isNotEmpty()) b.recycler.post { b.recycler.scrollToPosition(adapter.itemCount - 1) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentAiChatBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.ai_title)

        adapter = ChatAdapter(emptyList())
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        b.swContext.isChecked = false
        updateKeyBanner()
        updateEmptyState()

        b.btnSetKey.setOnClickListener { KeyDialog().show(childFragmentManager, "key") }
        b.btnSend.setOnClickListener { send() }
        b.btnNewChat.setOnClickListener { startNewConversation() }
        b.btnChats.setOnClickListener {
            ConversationListSheet { c -> openConversation(c.id) }.show(childFragmentManager, "convs")
        }
        b.chip1.setOnClickListener { b.etInput.setText(b.chip1.text) }
        b.chip2.setOnClickListener { b.etInput.setText(b.chip2.text) }
        b.chip3.setOnClickListener { b.etInput.setText(b.chip3.text) }

        parentFragmentManager.setFragmentResultListener(
            KeyDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, _ -> updateKeyBanner() }
    }

    private fun updateKeyBanner() {
        b.bannerNoKey.visibility =
            if (SecurePrefs.hasKey(requireContext())) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState() {
        val empty = adapter.itemCount == 0
        b.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        b.recycler.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun startNewConversation() {
        currentConversationId = null
        source?.removeObserver(messageObserver)
        source = null
        adapter.submit(emptyList())
        updateEmptyState()
        b.etInput.setText("")
    }

    private fun openConversation(id: Long) {
        currentConversationId = id
        source?.removeObserver(messageObserver)
        val s = repo.messages(id)
        source = s
        s.observe(viewLifecycleOwner, messageObserver)
    }

    private fun send() {
        if (sending) return
        val text = b.etInput.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return
        if (!SecurePrefs.hasKey(requireContext())) {
            toast("Add your OpenRouter API key first"); return
        }

        b.etInput.setText("")
        lifecycleScope.launch {
            var cid = currentConversationId
            if (cid == null) {
                cid = repo.newConversation(text.take(40))
                openConversation(cid)
            }
            repo.addMessage(MessageEntity(conversationId = cid, role = "user", text = text))
            repo.touchConversation(cid)
            requestReply(cid)
        }
    }

    private suspend fun requestReply(conversationId: Long) {
        val key = SecurePrefs.apiKey(requireContext())
        if (key.isBlank()) return
        setSending(true)
        try {
            val history = repo.messagesNow(conversationId)
            val request = mutableListOf<ChatMessage>()
            request.add(ChatMessage("system", SYSTEM_PROMPT))
            if (_b != null && b.swContext.isChecked) {
                contextSummary()?.let { request.add(ChatMessage("system", it)) }
            }
            history.takeLast(12).forEach { request.add(ChatMessage(it.role, it.text)) }

            val reply = OpenRouterClient(key).chat(request)
            repo.addMessage(
                MessageEntity(conversationId = conversationId, role = "assistant", text = reply)
            )
            repo.touchConversation(conversationId)
        } catch (e: Exception) {
            showError(e.message ?: "Request failed", conversationId)
        } finally {
            setSending(false)
        }
    }

    private fun setSending(value: Boolean) {
        sending = value
        if (_b == null) return
        b.progress.visibility = if (value) View.VISIBLE else View.GONE
        b.btnSend.isEnabled = !value
    }

    private fun showError(message: String, conversationId: Long) {
        if (_b == null) return
        Snackbar.make(b.root, "Cal AI: $message", Snackbar.LENGTH_LONG)
            .setAction("Retry") { lifecycleScope.launch { requestReply(conversationId) } }
            .show()
    }

    /** Compact summary of local data — only sent when the user turns the switch on. */
    private suspend fun contextSummary(): String? {
        val all = repo.expensesNow()
        if (all.isEmpty()) return null
        val month = Formatters.currentMonthKey()
        val monthItems = all.filter { Formatters.monthKey(it.date) == month }
        val expense = monthItems.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val income = monthItems.filter { it.type == "INCOME" }.sumOf { it.amount }
        val byCat = monthItems.filter { it.type == "EXPENSE" }
            .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }.take(5)
        val budgets = repo.budgetsNow().filter { it.month == month }
        val spentMap = monthItems.filter { it.type == "EXPENSE" }
            .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }

        return buildString {
            append("Local finance data for ").append(Formatters.monthLabel(month)).append(": ")
            append("expenses ").append(Formatters.money(expense))
            append(", income ").append(Formatters.money(income))
            append(", net ").append(Formatters.money(income - expense)).append(". ")
            if (byCat.isNotEmpty()) {
                append("Top expense categories: ")
                append(byCat.joinToString(", ") { "${it.first} ${Formatters.money(it.second)}" })
                append(". ")
            }
            if (budgets.isNotEmpty()) {
                append("Budgets: ")
                append(budgets.joinToString(", ") {
                    "${it.category} limit ${Formatters.money(it.amount)} " +
                        "spent ${Formatters.money(spentMap[it.category] ?: 0.0)}"
                })
                append(". ")
            }
        }
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        source?.removeObserver(messageObserver)
        source = null
        _b = null
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "You are Cal AI, the assistant inside the Cal-Expenses Android app. " +
                "Help with personal finance and productivity questions. Be concise; use short " +
                "paragraphs or brief lists. If a summary of the user's own data is provided, use " +
                "it and never invent figures that were not given. You are not a licensed financial " +
                "advisor; for significant decisions suggest consulting a qualified professional."
    }
}
