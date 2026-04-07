package dev.gavin.runqueue.jobs.api

data class RecurringScheduleResponse(
    val mode: RecurringScheduleMode,
    val interval: Int? = null,
    val intervalUnit: RecurringIntervalUnit? = null,
    val timeOfDay: String? = null,
    val dayOfWeek: String? = null,
    val dayOfMonth: Int? = null
)
