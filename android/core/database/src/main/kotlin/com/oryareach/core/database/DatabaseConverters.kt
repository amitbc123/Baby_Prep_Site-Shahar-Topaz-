package com.oryareach.core.database

import androidx.room.TypeConverter
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.Priority
import com.oryareach.core.model.SyncOperationType
import com.oryareach.core.model.SyncStatus
import com.oryareach.core.model.TaskCategory

/**
 * Enums are stored by name, not ordinal: an ordinal silently remaps every stored row if a
 * constant is ever inserted in the middle of a declaration.
 */
class DatabaseConverters {

    @TypeConverter fun priorityToString(value: Priority): String = value.name
    @TypeConverter fun stringToPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter fun assigneeToString(value: Assignee?): String? = value?.name
    @TypeConverter fun stringToAssignee(value: String?): Assignee? = value?.let(Assignee::valueOf)

    @TypeConverter fun taskCategoryToString(value: TaskCategory): String = value.name
    @TypeConverter fun stringToTaskCategory(value: String): TaskCategory = TaskCategory.valueOf(value)

    @TypeConverter fun syncStatusToString(value: SyncStatus): String = value.name
    @TypeConverter fun stringToSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter fun entityTypeToString(value: EntityType): String = value.wireName
    @TypeConverter fun stringToEntityType(value: String): EntityType =
        requireNotNull(EntityType.fromWireName(value)) { "unknown entity type: $value" }

    @TypeConverter fun operationToString(value: SyncOperationType): String = value.name
    @TypeConverter fun stringToOperation(value: String): SyncOperationType =
        SyncOperationType.valueOf(value)
}
