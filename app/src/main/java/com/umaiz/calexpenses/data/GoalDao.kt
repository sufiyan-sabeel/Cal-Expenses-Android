package com.umaiz.calexpenses.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun all(): LiveData<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    suspend fun allNow(): List<GoalEntity>

    @Insert suspend fun insert(g: GoalEntity): Long
    @Update suspend fun update(g: GoalEntity)
    @Delete suspend fun delete(g: GoalEntity)
    @Query("DELETE FROM goals") suspend fun clear()
}
