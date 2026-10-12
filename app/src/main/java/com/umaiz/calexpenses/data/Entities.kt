package com.umaiz.calexpenses.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,          // EXPENSE or INCOME
    val category: String,
    val note: String,
    val date: Long,            // epoch millis of the transaction
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val amount: Double,
    val month: String          // "yyyy-MM"
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val dueDate: Long? = null,
    val checklistJson: String = "[]",
    val noteId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** A single checklist row, serialised inside TaskEntity.checklistJson. */
data class ChecklistItem(val text: String, val done: Boolean)
