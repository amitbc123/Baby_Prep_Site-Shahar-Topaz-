package com.oryareach.feature.shopping

import androidx.annotation.StringRes
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.ShoppingCategory

@StringRes
internal fun ShoppingCategory.labelRes(): Int = when (this) {
    ShoppingCategory.NURSERY -> R.string.shopping_category_nursery
    ShoppingCategory.CLOTHING -> R.string.shopping_category_clothing
    ShoppingCategory.FEEDING -> R.string.shopping_category_feeding
    ShoppingCategory.CARE_AND_HEALTH -> R.string.shopping_category_care_and_health
    ShoppingCategory.SAFETY -> R.string.shopping_category_safety
    ShoppingCategory.MATERNITY_SUPPLIES -> R.string.shopping_category_maternity_supplies
    ShoppingCategory.OTHER -> R.string.shopping_category_other
}

@StringRes
internal fun Priority.labelRes(): Int = when (this) {
    Priority.LOW -> R.string.priority_low
    Priority.NORMAL -> R.string.priority_normal
    Priority.HIGH -> R.string.priority_high
}

@StringRes
internal fun Assignee?.labelRes(): Int = when (this) {
    Assignee.PARTNER_ONE -> R.string.assignee_partner_one
    Assignee.PARTNER_TWO -> R.string.assignee_partner_two
    Assignee.BOTH -> R.string.assignee_both
    null -> R.string.assignee_unassigned
}
