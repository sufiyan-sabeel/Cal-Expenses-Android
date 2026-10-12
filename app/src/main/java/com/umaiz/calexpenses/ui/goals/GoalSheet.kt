package com.umaiz.calexpenses.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.GoalEntity
import com.umaiz.calexpenses.databinding.SheetGoalBinding
import com.umaiz.calexpenses.util.Formatters
import kotlinx.coroutines.launch
import java.util.Calendar

class GoalSheet(private val existing: GoalEntity? = null) : BottomSheetDialogFragment() {

    private var _b: SheetGoalBinding? = null
    private val b get() = _b!!
    private var targetDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetGoalBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = (requireActivity().application as CalExpensesApp).repository

        if (existing != null) {
            b.tvTitle.text = "Edit goal"
            b.etTitle.setText(existing.title)
            b.etTarget.setText(existing.target.toString())
            b.etSaved.setText(existing.saved.toString())
            b.etNote.setText(existing.note)
            targetDate = existing.targetDate
            b.btnDelete.visibility = View.VISIBLE
        } else {
            b.tvTitle.text = "New goal"
            b.etSaved.setText("0")
        }
        updateDate()

        b.btnPickDate.setOnClickListener { pickDate() }
        b.btnClearDate.setOnClickListener { targetDate = null; updateDate() }

        b.btnSave.setOnClickListener {
            val title = b.etTitle.text?.toString()?.trim().orEmpty()
            if (title.isBlank()) { toast("Enter a goal name"); return@setOnClickListener }
            val target = b.etTarget.text?.toString()?.trim()?.toDoubleOrNull()
            if (target == null || target <= 0.0) {
                toast("Enter a target amount greater than zero"); return@setOnClickListener
            }
            val saved = b.etSaved.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            if (saved < 0.0) { toast("Saved amount cannot be negative"); return@setOnClickListener }
            val note = b.etNote.text?.toString()?.trim().orEmpty()

            b.btnSave.isEnabled = false
            lifecycleScope.launch {
                if (existing == null) {
                    repo.addGoal(GoalEntity(title = title, note = note, target = target,
                        saved = saved, targetDate = targetDate))
                } else {
                    repo.updateGoal(existing.copy(title = title, note = note, target = target,
                        saved = saved, targetDate = targetDate))
                }
                dismiss()
            }
        }

        b.btnDelete.setOnClickListener {
            val g = existing ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete goal?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch { repo.deleteGoal(g); dismiss() }
                }.show()
        }
    }

    private fun updateDate() {
        if (targetDate == null) {
            b.tvTargetDate.text = "No target date"
            b.btnClearDate.visibility = View.GONE
        } else {
            b.tvTargetDate.text = "Target: " + Formatters.date(targetDate!!)
            b.btnClearDate.visibility = View.VISIBLE
        }
    }

    private fun pickDate() {
        val c = Calendar.getInstance().apply { timeInMillis = targetDate ?: System.currentTimeMillis() }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 12, 0, 0); cal.set(Calendar.MILLISECOND, 0)
            targetDate = cal.timeInMillis; updateDate()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
