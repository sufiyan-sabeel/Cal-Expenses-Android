package com.umaiz.calexpenses.ui.transactions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.umaiz.calexpenses.data.ExpenseEntity
import com.umaiz.calexpenses.databinding.SheetExpenseBinding
import com.umaiz.calexpenses.util.Categories
import com.umaiz.calexpenses.util.Formatters
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseSheet(
    private val existing: ExpenseEntity? = null,
    private val initialType: String = "EXPENSE"
) : BottomSheetDialogFragment() {

    private var _b: SheetExpenseBinding? = null
    private val b get() = _b!!
    private var dateMillis: Long = System.currentTimeMillis()
    private var type: String = "EXPENSE"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetExpenseBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = (requireActivity().application as CalExpensesApp).repository

        if (existing != null) {
            type = existing.type
            dateMillis = existing.date
            b.tvTitle.text = "Edit transaction"
            b.etAmount.setText(existing.amount.toString())
            b.etNote.setText(existing.note)
            b.btnDelete.visibility = View.VISIBLE
        } else {
            b.tvTitle.text = "Add transaction"
            type = initialType
        }

        b.toggleType.check(
            when (type) {
                "INCOME" -> R.id.btnIncome
                "TRANSFER" -> R.id.btnTransfer
                else -> R.id.btnExpense
            }
        )
        updateCategories()
        updateDate()

        b.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                type = when (checkedId) {
                    R.id.btnIncome -> "INCOME"
                    R.id.btnTransfer -> "TRANSFER"
                    else -> "EXPENSE"
                }
                updateCategories()
            }
        }

        b.btnPickDate.setOnClickListener { pickDate() }
        b.btnPickTime.setOnClickListener { pickTime() }

        b.btnSave.setOnClickListener {
            val amount = b.etAmount.text?.toString()?.trim()?.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                toast("Enter a valid amount greater than zero"); return@setOnClickListener
            }
            val category = b.actCategory.text?.toString()?.trim().orEmpty()
            if (category.isBlank()) { toast("Choose a category"); return@setOnClickListener }
            val note = b.etNote.text?.toString()?.trim().orEmpty()

            b.btnSave.isEnabled = false
            lifecycleScope.launch {
                if (existing == null) {
                    repo.addExpense(
                        ExpenseEntity(
                            amount = amount, type = type, category = category,
                            note = note, date = dateMillis
                        )
                    )
                } else {
                    repo.updateExpense(
                        existing.copy(
                            amount = amount, type = type, category = category,
                            note = note, date = dateMillis
                        )
                    )
                }
                dismiss()
            }
        }

        b.btnDelete.setOnClickListener {
            val e = existing ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete transaction?")
                .setMessage("This cannot be undone.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch { repo.deleteExpense(e); dismiss() }
                }
                .show()
        }
    }

    private fun updateCategories() {
        val list = Categories.forType(type)
        b.actCategory.setAdapter(ArrayAdapter(requireContext(), R.layout.item_dropdown, list))
        val current = b.actCategory.text?.toString()
        if (current.isNullOrBlank() || current !in list) b.actCategory.setText(list.first(), false)
    }

    private fun updateDate() {
        val day = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dateMillis))
        b.tvDate.text = "$day · $time"
    }

    private fun pickDate() {
        val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
            dateMillis = cal.timeInMillis
            updateDate()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
        TimePickerDialog(requireContext(), { _, h, min ->
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            dateMillis = cal.timeInMillis
            updateDate()
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
