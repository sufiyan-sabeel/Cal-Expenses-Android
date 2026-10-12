package com.umaiz.calexpenses.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val target: Double,
    val saved: Double = 0.0,
    val targetDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 0..1, safe when target is zero or negative. */
    val progress: Float
        get() = if (target <= 0.0) 0f else (saved / target).toFloat().coerceIn(0f, 1f)

    val remaining: Double get() = (target - saved).coerceAtLeast(0.0)
    val isComplete: Boolean get() = target > 0.0 && saved >= target
}
