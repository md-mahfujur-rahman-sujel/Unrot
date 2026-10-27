package com.unrot.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val onboardingComplete: Boolean = false,
    val mathDifficulty: String = "MEDIUM",
    val extraMinutesPerUnlock: Int = 5,
    val baseProblemsCount: Int = 5,
    val protectionEnabled: Boolean = true,
    val strictModeEnabled: Boolean = false
)
