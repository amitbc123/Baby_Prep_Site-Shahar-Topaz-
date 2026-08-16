package com.oryareach.core.model

import kotlinx.serialization.Serializable

enum class ShoppingCategory {
    NURSERY,
    CLOTHING,
    FEEDING,
    CARE_AND_HEALTH,
    SAFETY,
    MATERNITY_SUPPLIES,
    OTHER,
}

enum class ShoppingStatus {
    NEED,
    ORDERED,
    BOUGHT,
}

@Serializable
data class ShoppingAlternative(
    val id: String,
    val name: String,
    val price: Int? = null,
    val link: String? = null,
    val note: String? = null,
)

@Serializable
data class ShoppingItem(
    val id: String,
    val name: String,
    val category: ShoppingCategory,
    val estimatedPrice: Int? = null,
    val actualPrice: Int? = null,
    val priority: Priority = Priority.NORMAL,
    val status: ShoppingStatus = ShoppingStatus.NEED,
    val assignee: Assignee? = null,
    val note: String? = null,
    val link: String? = null,
    val alternatives: List<ShoppingAlternative> = emptyList(),
    val chosenAlternativeId: String? = null,
)
