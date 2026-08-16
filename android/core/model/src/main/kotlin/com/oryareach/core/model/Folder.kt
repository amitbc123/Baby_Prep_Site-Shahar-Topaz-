package com.oryareach.core.model

import kotlinx.serialization.Serializable

/**
 * A folder in the couple's shared document tree.
 *
 * [path] is a materialized path of ancestor ids (e.g. `/root/child/`, ending in this folder's
 * own id) so a subtree query is a cheap `LIKE 'path%'` rather than a recursive one. It is
 * derived from [parentId] at write time, not user-editable.
 */
@Serializable
data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val path: String,
)
