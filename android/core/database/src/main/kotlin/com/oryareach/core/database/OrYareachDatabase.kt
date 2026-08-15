package com.oryareach.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.oryareach.core.database.dao.MenstrualCycleDao
import com.oryareach.core.database.dao.SyncOperationDao
import com.oryareach.core.database.dao.SyncStateDao
import com.oryareach.core.database.dao.TaskDao
import com.oryareach.core.database.entity.MenstrualCycleEntity
import com.oryareach.core.database.entity.SyncConflictEntity
import com.oryareach.core.database.entity.SyncCursorEntity
import com.oryareach.core.database.entity.SyncOperationEntity
import com.oryareach.core.database.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        MenstrualCycleEntity::class,
        SyncOperationEntity::class,
        SyncCursorEntity::class,
        SyncConflictEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class OrYareachDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun menstrualCycleDao(): MenstrualCycleDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "or-yareach.db"
    }
}
