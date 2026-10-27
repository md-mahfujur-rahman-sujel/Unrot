package com.unrot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unrot.app.data.local.entity.UnlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockEventDao {

    @Insert
    suspend fun insert(event: UnlockEventEntity)

    @Query("SELECT COUNT(*) FROM unlock_events WHERE timestamp >= :startOfDay")
    suspend fun countToday(startOfDay: Long): Int

    @Query("SELECT * FROM unlock_events WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun observeToday(startOfDay: Long): Flow<List<UnlockEventEntity>>
}
