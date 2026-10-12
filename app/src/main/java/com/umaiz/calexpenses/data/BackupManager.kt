package com.umaiz.calexpenses.data

import android.content.Context
import android.net.Uri
import com.umaiz.calexpenses.util.Formatters
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Handles offline JSON backup / restore and CSV export of expenses.
 * All IO happens against a Storage Access Framework [Uri] supplied by the caller,
 * so no storage permission is required.
 */
class BackupManager(private val context: Context, private val repo: Repository) {

    // ---------------------------------------------------------------- JSON export
    suspend fun exportJson(uri: Uri) {
        val root = JSONObject()
        root.put("app", "Cal-Expenses")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val exp = JSONArray()
        repo.expensesNow().forEach { e ->
            exp.put(JSONObject().apply {
                put("amount", e.amount)
                put("type", e.type)
                put("category", e.category)
                put("note", e.note)
                put("date", e.date)
                put("createdAt", e.createdAt)
            })
        }
        root.put("expenses", exp)

        val bud = JSONArray()
        repo.budgetsNow().forEach { b ->
            bud.put(JSONObject().apply {
                put("category", b.category); put("amount", b.amount); put("month", b.month)
            })
        }
        root.put("budgets", bud)

        val notes = JSONArray()
        repo.notesNow().forEach { n ->
            notes.put(JSONObject().apply {
                put("title", n.title); put("text", n.text)
                put("audioPath", n.audioPath ?: JSONObject.NULL)
                put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
            })
        }
        root.put("notes", notes)

        val tasks = JSONArray()
        repo.tasksNow().forEach { t ->
            tasks.put(JSONObject().apply {
                put("title", t.title); put("done", t.done)
                put("dueDate", t.dueDate ?: JSONObject.NULL)
                put("checklistJson", t.checklistJson)
                put("noteId", t.noteId ?: JSONObject.NULL)
                put("createdAt", t.createdAt)
            })
        }
        root.put("tasks", tasks)

        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(root.toString(2).toByteArray(Charsets.UTF_8))
            os.flush()
        } ?: throw IllegalStateException("Could not open destination file")
    }

    // ---------------------------------------------------------------- CSV export
    suspend fun exportCsv(uri: Uri) {
        val sb = StringBuilder()
        sb.append("Date,Type,Category,Amount,Note\n")
        repo.expensesNow().forEach { e ->
            sb.append(csv(Formatters.date(e.date))).append(',')
                .append(csv(e.type)).append(',')
                .append(csv(e.category)).append(',')
                .append(String.format(java.util.Locale.US, "%.2f", e.amount)).append(',')
                .append(csv(e.note)).append('\n')
        }
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(sb.toString().toByteArray(Charsets.UTF_8))
            os.flush()
        } ?: throw IllegalStateException("Could not open destination file")
    }

    private fun csv(v: String): String {
        val needsQuote = v.contains(',') || v.contains('"') || v.contains('\n')
        val escaped = v.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    // ---------------------------------------------------------------- JSON import
    /**
     * Validates and parses a backup file. Throws [IllegalArgumentException] with a
     * human-readable message if the file is not a valid Cal-Expenses backup.
     * Existing data is NOT touched here — the caller confirms before [applyImport].
     */
    fun parseBackup(uri: Uri): ParsedBackup {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: throw IllegalArgumentException("Could not read the selected file")

        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a valid JSON backup file")
        }

        val app = root.optString("app", "")
        if (app != "Cal-Expenses") {
            throw IllegalArgumentException("This file is not a Cal-Expenses backup")
        }

        val expenses = mutableListOf<ExpenseEntity>()
        root.optJSONArray("expenses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val amount = o.optDouble("amount", Double.NaN)
                if (amount.isNaN() || amount < 0) continue          // skip malformed rows
                val type = if (o.optString("type") == "INCOME") "INCOME" else "EXPENSE"
                expenses.add(
                    ExpenseEntity(
                        amount = amount, type = type,
                        category = o.optString("category", "Other"),
                        note = o.optString("note", ""),
                        date = o.optLong("date", System.currentTimeMillis()),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val budgets = mutableListOf<BudgetEntity>()
        root.optJSONArray("budgets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val amount = o.optDouble("amount", Double.NaN)
                val month = o.optString("month", "")
                if (amount.isNaN() || amount < 0 || month.isEmpty()) continue
                budgets.add(BudgetEntity(category = o.optString("category", "Other"), amount = amount, month = month))
            }
        }

        val notes = mutableListOf<NoteEntity>()
        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                notes.add(
                    NoteEntity(
                        title = o.optString("title", "Untitled"),
                        text = o.optString("text", ""),
                        audioPath = if (o.isNull("audioPath")) null else o.optString("audioPath"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val tasks = mutableListOf<TaskEntity>()
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tasks.add(
                    TaskEntity(
                        title = o.optString("title", "Untitled"),
                        done = o.optBoolean("done", false),
                        dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
                        checklistJson = o.optString("checklistJson", "[]"),
                        noteId = if (o.isNull("noteId")) null else o.optLong("noteId"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return ParsedBackup(expenses, budgets, notes, tasks)
    }

    /** Replaces all local data with the parsed backup. Destructive — call only after confirmation. */
    suspend fun applyImport(parsed: ParsedBackup) {
        repo.replaceExpenses(parsed.expenses)
        repo.replaceBudgets(parsed.budgets)
        repo.replaceNotes(parsed.notes)
        repo.replaceTasks(parsed.tasks)
    }
}

data class ParsedBackup(
    val expenses: List<ExpenseEntity>,
    val budgets: List<BudgetEntity>,
    val notes: List<NoteEntity>,
    val tasks: List<TaskEntity>
) {
    val totalCount: Int get() = expenses.size + budgets.size + notes.size + tasks.size
}
