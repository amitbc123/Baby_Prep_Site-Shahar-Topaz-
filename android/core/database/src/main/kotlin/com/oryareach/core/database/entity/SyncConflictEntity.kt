package com.oryareach.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.oryareach.core.model.EntityType

/**
 * A local edit the server rejected because someone else got there first.
 *
 * The server's version is parked here, still encrypted, until a person chooses which side
 * wins. Nothing is applied automatically — resolving it silently is how a partner's edit
 * disappears without either of them noticing.
 */
@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "entity_type") val entityType: EntityType,
    @ColumnInfo(name = "server_ciphertext") val serverCiphertext: ByteArray,
    @ColumnInfo(name = "server_version") val serverVersion: Int,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
) {
    // ByteArray defaults to identity equality, which would make every comparison wrong.
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is SyncConflictEntity &&
                recordId == other.recordId &&
                entityType == other.entityType &&
                serverCiphertext.contentEquals(other.serverCiphertext) &&
                serverVersion == other.serverVersion &&
                serverUpdatedAt == other.serverUpdatedAt &&
                detectedAt == other.detectedAt
            )

    override fun hashCode(): Int {
        var result = recordId.hashCode()
        result = 31 * result + entityType.hashCode()
        result = 31 * result + serverCiphertext.contentHashCode()
        result = 31 * result + serverVersion
        result = 31 * result + serverUpdatedAt.hashCode()
        result = 31 * result + detectedAt.hashCode()
        return result
    }
}
