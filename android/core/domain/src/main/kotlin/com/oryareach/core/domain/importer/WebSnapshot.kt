package com.oryareach.core.domain.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors `AppSnapshot` from the web app's `src/types/models.ts` — the export/import JSON shape. */
@Serializable
data class WebSnapshot(
    val version: Int,
    val settings: WebSettings,
    val shoppingItems: List<WebShoppingItem> = emptyList(),
    val tasks: List<WebTask> = emptyList(),
    val importantDates: List<WebImportantDate> = emptyList(),
)

@Serializable
data class WebSettings(
    val dueDate: String,
    val babyName: String? = null,
)

@Serializable
data class WebAlternative(
    val id: String,
    val name: String,
    val price: Double? = null,
    val link: String? = null,
    val note: String? = null,
)

@Serializable
data class WebShoppingItem(
    val id: String,
    val name: String,
    val category: String,
    val estimatedPrice: Double? = null,
    val actualPrice: Double? = null,
    val priority: String,
    val status: String,
    val assignee: String? = null,
    val note: String? = null,
    val link: String? = null,
    val alternatives: List<WebAlternative> = emptyList(),
    val chosenAlternativeId: String? = null,
)

@Serializable
data class WebTask(
    val id: String,
    val title: String,
    val category: String,
    val dueDate: String? = null,
    val priority: String,
    val assignee: String? = null,
    val done: Boolean,
    val note: String? = null,
)

@Serializable
data class WebImportantDate(
    val id: String,
    val date: String,
    val title: String,
    @SerialName("wish") val wish: String? = null,
)
