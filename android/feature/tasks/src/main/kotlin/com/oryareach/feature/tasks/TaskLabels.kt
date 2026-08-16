package com.oryareach.feature.tasks

import androidx.annotation.StringRes
import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.RecurrenceFrequency
import com.oryareach.core.model.TaskCategory

@StringRes
internal fun TaskCategory.labelRes(): Int = when (this) {
    TaskCategory.HOME_PREP -> R.string.task_category_home_prep
    TaskCategory.DOCUMENTS_AND_INSURANCE -> R.string.task_category_documents_and_insurance
    TaskCategory.MEDICAL -> R.string.task_category_medical
    TaskCategory.HOSPITAL_BAG -> R.string.task_category_hospital_bag
    TaskCategory.OTHER -> R.string.task_category_other
}

@StringRes
internal fun Priority.labelRes(): Int = when (this) {
    Priority.LOW -> R.string.priority_low
    Priority.NORMAL -> R.string.priority_normal
    Priority.HIGH -> R.string.priority_high
}

@StringRes
internal fun RecurrenceFrequency?.recurrenceLabelRes(): Int = when (this) {
    null -> R.string.tasks_recurrence_none
    RecurrenceFrequency.DAILY -> R.string.tasks_recurrence_daily
    RecurrenceFrequency.WEEKLY -> R.string.tasks_recurrence_weekly
    RecurrenceFrequency.MONTHLY -> R.string.tasks_recurrence_monthly
}

@StringRes
internal fun Assignee?.labelRes(): Int = when (this) {
    Assignee.PARTNER_ONE -> R.string.assignee_partner_one
    Assignee.PARTNER_TWO -> R.string.assignee_partner_two
    Assignee.BOTH -> R.string.assignee_both
    null -> R.string.assignee_unassigned
}
