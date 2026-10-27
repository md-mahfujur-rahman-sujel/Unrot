package com.unrot.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "limited_apps")
data class LimitedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyBudgetMinutes: Int,
    val usedTodayMs: Long = 0L,
    val extraTimeMs: Long = 0L,
    val unlockCountToday: Int = 0,
    val lastResetDate: String = "",
    val isEnabled: Boolean = true,
    val scheduleStartMinutes: Int? = null,
    val scheduleEndMinutes: Int? = null,
    val scheduleDays: String? = null,
    // User-chosen "show this on the Home dashboard" flag, set from the app picker — independent
    // of isEnabled/limited so a user with many limited apps can curate a short, relevant Home list.
    val isFavorite: Boolean = false,
    val isWhitelisted: Boolean = false,
    val appGroup: String = "USER"
)
