package com.oryareach.core.model

/**
 * Local lifecycle of a record relative to the server. Records are never hard-deleted
 * locally until the server has acknowledged the delete, so a tombstone can be replayed.
 */
enum class SyncStatus {
    LOCAL_ONLY,
    PENDING_UPLOAD,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED,
    CONFLICT,
}
