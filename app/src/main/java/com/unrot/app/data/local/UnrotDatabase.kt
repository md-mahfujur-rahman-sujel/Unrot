package com.unrot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unrot.app.data.local.dao.AppSettingsDao
import com.unrot.app.data.local.dao.LimitedAppDao
import com.unrot.app.data.local.dao.BlockAttemptDao
import com.unrot.app.data.local.dao.UnlockEventDao
import com.unrot.app.data.local.dao.UsageLogDao
import com.unrot.app.data.local.entity.AppSettingsEntity
import com.unrot.app.data.local.entity.BlockAttemptEntity
import com.unrot.app.data.local.entity.LimitedAppEntity
import com.unrot.app.data.local.entity.UnlockEventEntity
import com.unrot.app.data.local.entity.UsageLogEntity

@Database(
    entities = [
        LimitedAppEntity::class,
        UsageLogEntity::class,
        UnlockEventEntity::class,
        AppSettingsEntity::class,
        BlockAttemptEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class UnrotDatabase : RoomDatabase() {
    abstract fun limitedAppDao(): LimitedAppDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun unlockEventDao(): UnlockEventDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun blockAttemptDao(): BlockAttemptDao
}
