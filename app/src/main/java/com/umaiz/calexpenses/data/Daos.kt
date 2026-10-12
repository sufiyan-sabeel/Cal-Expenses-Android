package com.umaiz.calexpenses.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun all(): LiveData<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    suspend fun allNow(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE (note LIKE '%' || :q || '%' OR category LIKE '%' || :q || '%') ORDER BY date DESC, id DESC")
    fun search(q: String): LiveData<List<ExpenseEntity>>

    @Insert suspend fun insert(e: ExpenseEntity): Long
    @Update suspend fun update(e: ExpenseEntity)
    @Delete suspend fun delete(e: ExpenseEntity)
    @Query("DELETE FROM expenses") suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY category")
    fun forMonth(month: String): LiveData<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun allNow(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(b: BudgetEntity): Long
    @Update suspend fun update(b: BudgetEntity)
    @Delete suspend fun delete(b: BudgetEntity)
    @Query("DELETE FROM budgets") suspend fun clear()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun all(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun allNow(): List<NoteEntity>

    @Insert suspend fun insert(n: NoteEntity): Long
    @Update suspend fun update(n: NoteEntity)
    @Delete suspend fun delete(n: NoteEntity)
    @Query("DELETE FROM notes") suspend fun clear()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, (dueDate IS NULL) ASC, dueDate ASC, createdAt DESC")
    fun all(): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY done ASC, (dueDate IS NULL) ASC, dueDate ASC, createdAt DESC")
    suspend fun allNow(): List<TaskEntity>

    @Insert suspend fun insert(t: TaskEntity): Long
    @Update suspend fun update(t: TaskEntity)
    @Delete suspend fun delete(t: TaskEntity)
    @Query("DELETE FROM tasks") suspend fun clear()
}
