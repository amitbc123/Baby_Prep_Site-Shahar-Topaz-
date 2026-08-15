package com.oryareach.core.model

/**
 * Ordered least to most urgent so `compareTo` sorts the way the UI wants.
 *
 * The web app stored the Hebrew label as the value itself; here the label is a string
 * resource resolved per locale, because the Android app is bilingual.
 */
enum class Priority {
    LOW,
    NORMAL,
    HIGH,
}

/**
 * Which member of the couple an item belongs to. Stored positionally rather than by name so
 * the display name can come from settings and be changed without rewriting every record.
 */
enum class Assignee {
    PARTNER_ONE,
    PARTNER_TWO,
    BOTH,
}
