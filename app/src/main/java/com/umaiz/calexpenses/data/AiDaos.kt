package com.umaiz.calexpenses.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun all(): LiveData<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun allNow(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun byId(id: Long): ConversationEntity?

    @Insert suspend fun insert(c: ConversationEntity): Long
    @Update suspend fun update(c: ConversationEntity)
    @Query("UPDATE conversations SET updatedAt = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long)
    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)
    @Delete suspend fun delete(c: ConversationEntity)
    @Query("DELETE FROM conversations") suspend fun clear()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC, id ASC")
    fun forConversation(cid: Long): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY createdAt ASC, id ASC")
    suspend fun forConversationNow(cid: Long): List<MessageEntity>

    @Insert suspend fun insert(m: MessageEntity): Long
    @Query("DELETE FROM messages WHERE conversationId = :cid") suspend fun clearFor(cid: Long)
    @Query("DELETE FROM messages") suspend fun clear()
}
