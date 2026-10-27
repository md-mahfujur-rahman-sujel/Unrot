package com.unrot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.unrot.app.data.local.entity.LimitedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LimitedAppDao {

    @Query("SELECT * FROM limited_apps WHERE isEnabled = 1")
    fun observeEnabledApps(): Flow<List<LimitedAppEntity>>

    @Query("SELECT * FROM limited_apps WHERE isEnabled = 1 AND isFavorite = 1")
    fun observeFavoriteApps(): Flow<List<LimitedAppEntity>>

    @Query("SELECT * FROM limited_apps")
    fun observeAllApps(): Flow<List<LimitedAppEntity>>

    @Query("SELECT * FROM limited_apps WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): LimitedAppEntity?

    @Query("SELECT * FROM limited_apps WHERE isEnabled = 1")
    suspend fun getEnabledApps(): List<LimitedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: LimitedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<LimitedAppEntity>)

    @Update
    suspend fun update(app: LimitedAppEntity)

    @Query("DELETE FROM limited_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE limited_apps SET usedTodayMs = 0, extraTimeMs = 0, unlockCountToday = 0, lastResetDate = :date")
    suspend fun resetAllDaily(date: String)

    @Query("UPDATE limited_apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)

    @Query("UPDATE limited_apps SET isWhitelisted = :isWhitelisted WHERE packageName = :packageName")
    suspend fun setWhitelisted(packageName: String, isWhitelisted: Boolean)
}
