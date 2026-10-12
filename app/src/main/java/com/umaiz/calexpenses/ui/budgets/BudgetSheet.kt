package com.umaiz.calexpenses.ui.budgets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.BudgetEntity
import com.umaiz.calexpenses.databinding.SheetBudgetBinding
import com.umaiz.calexpenses.util.Categories
import com.umaiz.calexpenses.util.Formatters
import kotlinx.coroutines.launch

class BudgetSheet(
    private val month: String,
    private val existing: BudgetEntity? = null
) : BottomSheetDialogFragment() {

    private var _b: SheetBudgetBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetBudgetBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = (requireActivity().application as CalExpensesApp).repository
        val list = Categories.EXPENSE
        b.actCategory.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, list))

        b.tvMonth.text = "For " + Formatters.monthLabel(month)

        if (existing != null) {
            b.tvTitle.text = "Edit budget"
            b.actCategory.setText(existing.category, false)
            b.etAmount.setText(existing.amount.toString())
            b.btnDelete.visibility = View.VISIBLE
        } else {
            b.tvTitle.text = "New budget"
            b.actCategory.setText(list.first(), false)
        }

        b.btnSave.setOnClickListener {
            val amount = b.etAmount.text?.toString()?.trim()?.toDoubleOrNull()
            if (amount == null || amount <= 0.0) { toast("Enter a valid limit"); return@setOnClickListener }
            val category = b.actCategory.text?.toString()?.trim().orEmpty()
            if (category.isBlank()) { toast("Choose a category"); return@setOnClickListener }

            lifecycleScope.launch {
                if (existing == null) {
                    repo.addBudget(BudgetEntity(category = category, amount = amount, month = month))
                } else {
                    repo.addBudget(existing.copy(category = category, amount = amount, month = month))
                }
                dismiss()
            }
        }

        b.btnDelete.setOnClickListener {
            val e = existing ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete budget?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch { repo.deleteBudget(e); dismiss() }
                }.show()
        }
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
