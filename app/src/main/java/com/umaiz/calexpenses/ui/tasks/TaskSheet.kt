package com.umaiz.calexpenses.ui.tasks

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.data.ChecklistItem
import com.umaiz.calexpenses.data.TaskEntity
import com.umaiz.calexpenses.databinding.SheetTaskBinding
import com.umaiz.calexpenses.util.Checklist
import com.umaiz.calexpenses.util.Formatters
import com.umaiz.calexpenses.util.Notifications
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskSheet(private val existing: TaskEntity? = null) : BottomSheetDialogFragment() {

    private var _b: SheetTaskBinding? = null
    private val b get() = _b!!
    private var dueDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetTaskBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = (requireActivity().application as CalExpensesApp).repository

        if (existing != null) {
            b.tvTitle.text = "Edit task"
            b.etTitle.setText(existing.title)
            dueDate = existing.dueDate
            b.btnDelete.visibility = View.VISIBLE
            Checklist.fromJson(existing.checklistJson).forEach { addRow(it.text, it.done) }
        } else {
            b.tvTitle.text = "New task"
        }
        updateDue()

        b.btnPickDate.setOnClickListener { pickDate() }
        b.btnClearDate.setOnClickListener { dueDate = null; updateDue() }
        b.btnAddItem.setOnClickListener { addRow("", false) }

        b.btnSave.setOnClickListener {
            val title = b.etTitle.text?.toString()?.trim().orEmpty()
            if (title.isBlank()) { toast("Enter a task name"); return@setOnClickListener }
            val items = collectChecklist()
            val json = Checklist.toJson(items)
            val notify = b.swNotify.isChecked
            val due = dueDate
            val appCtx = requireContext().applicationContext

            lifecycleScope.launch {
                if (existing == null) {
                    val id = repo.addTask(
                        TaskEntity(title = title, dueDate = due, checklistJson = json)
                    )
                    if (notify && due != null) scheduleReminder(appCtx, id.toInt(), title, due)
                } else {
                    repo.updateTask(existing.copy(title = title, dueDate = due, checklistJson = json))
                    if (notify && due != null) scheduleReminder(appCtx, existing.id.toInt(), title, due)
                    else Notifications.cancel(appCtx, existing.id.toInt())
                }
                dismiss()
            }
        }

        b.btnDelete.setOnClickListener {
            val t = existing ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete task?")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    Notifications.cancel(requireContext().applicationContext, t.id.toInt())
                    lifecycleScope.launch { repo.deleteTask(t); dismiss() }
                }.show()
        }
    }

    private fun scheduleReminder(ctx: android.content.Context, id: Int, title: String, due: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = due
            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        var at = cal.timeInMillis
        if (at <= System.currentTimeMillis()) at = System.currentTimeMillis() + 5000
        Notifications.schedule(ctx, id, "Task due: $title", at)
    }

    private fun addRow(text: String, done: Boolean) {
        val row = layoutInflater.inflate(R.layout.item_checklist_edit, b.checklist, false)
        val cb = row.findViewById<CheckBox>(R.id.cbItem)
        val et = row.findViewById<EditText>(R.id.etItem)
        val rm = row.findViewById<ImageView>(R.id.btnRemove)
        cb.isChecked = done
        et.setText(text)
        rm.setOnClickListener { b.checklist.removeView(row) }
        b.checklist.addView(row)
    }

    private fun collectChecklist(): List<ChecklistItem> {
        val out = mutableListOf<ChecklistItem>()
        for (i in 0 until b.checklist.childCount) {
            val row = b.checklist.getChildAt(i)
            val text = row.findViewById<EditText>(R.id.etItem).text?.toString()?.trim().orEmpty()
            val done = row.findViewById<CheckBox>(R.id.cbItem).isChecked
            if (text.isNotBlank()) out.add(ChecklistItem(text, done))
        }
        return out
    }

    private fun updateDue() {
        if (dueDate == null) {
            b.tvDue.text = "No due date"
            b.btnClearDate.visibility = View.GONE
        } else {
            b.tvDue.text = "Due: " + Formatters.date(dueDate!!)
            b.btnClearDate.visibility = View.VISIBLE
        }
    }

    private fun pickDate() {
        val c = Calendar.getInstance()
        c.timeInMillis = dueDate ?: System.currentTimeMillis()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val cal = Calendar.getInstance(); cal.set(y, m, d)
            cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            dueDate = cal.timeInMillis; updateDue()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
