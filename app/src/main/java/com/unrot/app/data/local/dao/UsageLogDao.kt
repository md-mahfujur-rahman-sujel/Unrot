package com.unrot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.unrot.app.data.local.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {

    @Insert
    suspend fun insert(log: UsageLogEntity)

    @Query("SELECT * FROM usage_logs WHERE date >= :startDate ORDER BY date DESC")
    fun observeSince(startDate: String): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_logs WHERE packageName = :packageName AND date >= :startDate")
    suspend fun getForAppSince(packageName: String, startDate: String): List<UsageLogEntity>

    @Query("SELECT SUM(usageMs) FROM usage_logs WHERE packageName = :packageName AND date >= :startDate")
    suspend fun totalUsageForApp(packageName: String, startDate: String): Long?
}
