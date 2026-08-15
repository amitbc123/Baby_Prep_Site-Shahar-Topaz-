package com.oryareach.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ImportantDate(
    val id: String,
    val date: LocalDate,
    val title: String,
    val wish: String? = null,
)
