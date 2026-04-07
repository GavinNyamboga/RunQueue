package dev.gavin.runqueue.jobs.api

data class ScheduleOptionsResponse(
    val recurringModes: List<RecurringScheduleMode>,
    val intervalUnits: List<RecurringIntervalUnit>
)
