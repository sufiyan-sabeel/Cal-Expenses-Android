package com.umaiz.calexpenses.ui.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.GoalEntity
import com.umaiz.calexpenses.databinding.DialogAddMoneyBinding
import com.umaiz.calexpenses.databinding.FragmentGoalsBinding
import com.umaiz.calexpenses.util.Formatters
import kotlinx.coroutines.launch

class GoalsFragment : Fragment() {

    private var _b: FragmentGoalsBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository
    private lateinit var adapter: GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentGoalsBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.nav_goals)

        adapter = GoalAdapter(
            emptyList(),
            onClick = { g -> GoalSheet(g).show(childFragmentManager, "goal") },
            onAddMoney = { g -> addMoney(g) }
        )
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter
        b.fabAdd.setOnClickListener { GoalSheet(null).show(childFragmentManager, "goal") }

        repo.goals.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            val saved = list.sumOf { it.saved }
            val target = list.sumOf { it.target }
            b.tvTotalSaved.text = Formatters.money(saved)
            b.tvTotalSub.text = "of ${Formatters.money(target)} across ${list.size} goal(s)"
        }
    }

    private fun addMoney(goal: GoalEntity) {
        val binding = DialogAddMoneyBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add money to ${goal.title}")
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Add") { _, _ ->
                val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    repo.updateGoal(goal.copy(saved = goal.saved + amount))
                }
            }
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
