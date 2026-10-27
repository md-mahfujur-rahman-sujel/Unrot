package com.unrot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unrot.app.data.local.entity.BlockAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockAttemptDao {

    @Insert
    suspend fun insert(attempt: BlockAttemptEntity)

    @Query("SELECT COUNT(*) FROM block_attempts WHERE timestamp >= :startOfDay")
    suspend fun countToday(startOfDay: Long): Int

    @Query("SELECT * FROM block_attempts WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun observeSince(startOfDay: Long): Flow<List<BlockAttemptEntity>>

    @Query(
        """
        SELECT packageName, appName, COUNT(*) as attempts
        FROM block_attempts
        WHERE timestamp >= :startOfDay
        GROUP BY packageName
        ORDER BY attempts DESC
        """
    )
    suspend fun countsByAppToday(startOfDay: Long): List<BlockAttemptCount>

    @Query("DELETE FROM block_attempts WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

data class BlockAttemptCount(
    val packageName: String,
    val appName: String,
    val attempts: Int
)
