package com.umaiz.calexpenses.data

import androidx.lifecycle.LiveData

class Repository(private val db: AppDatabase) {

    val expenses: LiveData<List<ExpenseEntity>> = db.expenseDao().all()
    val notes: LiveData<List<NoteEntity>> = db.noteDao().all()
    val tasks: LiveData<List<TaskEntity>> = db.taskDao().all()
    val conversations: LiveData<List<ConversationEntity>> = db.conversationDao().all()
    val goals: LiveData<List<GoalEntity>> = db.goalDao().all()

    fun searchExpenses(q: String): LiveData<List<ExpenseEntity>> = db.expenseDao().search(q)
    fun budgetsForMonth(month: String): LiveData<List<BudgetEntity>> = db.budgetDao().forMonth(month)

    suspend fun addExpense(e: ExpenseEntity) = db.expenseDao().insert(e)
    suspend fun updateExpense(e: ExpenseEntity) = db.expenseDao().update(e)
    suspend fun deleteExpense(e: ExpenseEntity) = db.expenseDao().delete(e)
    suspend fun expensesNow(): List<ExpenseEntity> = db.expenseDao().allNow()

    suspend fun addBudget(b: BudgetEntity) = db.budgetDao().insert(b)
    suspend fun deleteBudget(b: BudgetEntity) = db.budgetDao().delete(b)
    suspend fun budgetsNow(): List<BudgetEntity> = db.budgetDao().allNow()

    suspend fun addNote(n: NoteEntity) = db.noteDao().insert(n)
    suspend fun updateNote(n: NoteEntity) = db.noteDao().update(n)
    suspend fun deleteNote(n: NoteEntity) = db.noteDao().delete(n)
    suspend fun notesNow(): List<NoteEntity> = db.noteDao().allNow()

    suspend fun addTask(t: TaskEntity) = db.taskDao().insert(t)
    suspend fun updateTask(t: TaskEntity) = db.taskDao().update(t)
    suspend fun deleteTask(t: TaskEntity) = db.taskDao().delete(t)
    suspend fun tasksNow(): List<TaskEntity> = db.taskDao().allNow()

    // ---------------- Cal AI chat ----------------
    fun messages(conversationId: Long): LiveData<List<MessageEntity>> =
        db.messageDao().forConversation(conversationId)

    suspend fun newConversation(title: String): Long =
        db.conversationDao().insert(ConversationEntity(title = title))

    suspend fun addMessage(message: MessageEntity): Long = db.messageDao().insert(message)

    suspend fun messagesNow(conversationId: Long): List<MessageEntity> =
        db.messageDao().forConversationNow(conversationId)

    suspend fun conversationById(id: Long): ConversationEntity? = db.conversationDao().byId(id)

    suspend fun conversationsNow(): List<ConversationEntity> = db.conversationDao().allNow()

    suspend fun touchConversation(id: Long) =
        db.conversationDao().touch(id, System.currentTimeMillis())

    suspend fun renameConversation(id: Long, title: String) =
        db.conversationDao().rename(id, title)

    suspend fun deleteConversation(conversation: ConversationEntity) {
        db.messageDao().clearFor(conversation.id)
        db.conversationDao().delete(conversation)
    }

    suspend fun clearAllChats() {
        db.messageDao().clear()
        db.conversationDao().clear()
    }

    // ---------------- goals ----------------
    suspend fun addGoal(g: GoalEntity): Long = db.goalDao().insert(g)
    suspend fun updateGoal(g: GoalEntity) = db.goalDao().update(g)
    suspend fun deleteGoal(g: GoalEntity) = db.goalDao().delete(g)
    suspend fun goalsNow(): List<GoalEntity> = db.goalDao().allNow()

    // ---- bulk operations used by backup import (destructive, guarded by caller) ----
    suspend fun replaceExpenses(list: List<ExpenseEntity>) {
        db.expenseDao().clear(); list.forEach { db.expenseDao().insert(it.copy(id = 0)) }
    }
    suspend fun replaceBudgets(list: List<BudgetEntity>) {
        db.budgetDao().clear(); list.forEach { db.budgetDao().insert(it.copy(id = 0)) }
    }
    suspend fun replaceNotes(list: List<NoteEntity>) {
        db.noteDao().clear(); list.forEach { db.noteDao().insert(it.copy(id = 0)) }
    }
    suspend fun replaceTasks(list: List<TaskEntity>) {
        db.taskDao().clear(); list.forEach { db.taskDao().insert(it.copy(id = 0)) }
    }
}
