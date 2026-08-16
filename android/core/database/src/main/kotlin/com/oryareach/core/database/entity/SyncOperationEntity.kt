package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.oryareach.core.model.EntityType
import com.oryareach.core.model.SyncOperationType

/**
 * The outbox. A local write enqueues one of these; the sync worker drains it.
 *
 * Keeping the queue in the same database as the data means a write and its outbox entry
 * commit in one transaction — there is no window where a record is saved but its upload was
 * never scheduled.
 */
@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["entity_type", "record_id"]),
    ],
)
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "entity_type") val entityType: EntityType,
    val operation: SyncOperationType,
    /** Echoed to the server so a retry after a lost response is not applied twice. */
    @ColumnInfo(name = "client_mutation_id") val clientMutationId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
)
