package dev.gavin.runqueue.jobs.application

import dev.gavin.runqueue.common.api.ValidationException
import dev.gavin.runqueue.jobs.api.RecurringIntervalUnit
import dev.gavin.runqueue.jobs.api.RecurringScheduleMode
import dev.gavin.runqueue.jobs.api.RecurringScheduleRequest
import dev.gavin.runqueue.jobs.api.RecurringScheduleResponse
import java.time.DayOfWeek
import java.time.LocalTime

object RecurringScheduleMapper {
    fun toCronExpression(schedule: RecurringScheduleRequest): String {
        return when (schedule.mode) {
            RecurringScheduleMode.EVERY -> buildEveryCron(schedule)
            RecurringScheduleMode.DAILY -> {
                val time = requireTimeOfDay(schedule.timeOfDay)
                "0 ${time.minute} ${time.hour} * * *"
            }
            RecurringScheduleMode.WEEKLY -> {
                val time = requireTimeOfDay(schedule.timeOfDay)
                val dayOfWeek = requireDayOfWeek(schedule.dayOfWeek)
                "0 ${time.minute} ${time.hour} * * ${dayOfWeek.value % 7}"
            }
            RecurringScheduleMode.MONTHLY -> {
                val time = requireTimeOfDay(schedule.timeOfDay)
                val dayOfMonth = requireDayOfMonth(schedule.dayOfMonth)
                "0 ${time.minute} ${time.hour} $dayOfMonth * *"
            }
        }
    }

    fun fromCronExpression(cronExpression: String?): RecurringScheduleResponse? {
        if (cronExpression.isNullOrBlank()) return null
        val parts = cronExpression.trim().split(Regex("\\s+"))
        if (parts.size != 6 || parts[0] != "0") return null

        return when {
            parts[1].startsWith("*/") && parts[2] == "*" && parts[3] == "*" && parts[4] == "*" && parts[5] == "*" ->
                RecurringScheduleResponse(
                    mode = RecurringScheduleMode.EVERY,
                    interval = parts[1].removePrefix("*/").toIntOrNull(),
                    intervalUnit = RecurringIntervalUnit.MINUTES
                )

            parts[1] == "0" && parts[2].startsWith("*/") && parts[3] == "*" && parts[4] == "*" && parts[5] == "*" ->
                RecurringScheduleResponse(
                    mode = RecurringScheduleMode.EVERY,
                    interval = parts[2].removePrefix("*/").toIntOrNull(),
                    intervalUnit = RecurringIntervalUnit.HOURS
                )

            parts[3] == "*" && parts[4] == "*" && parts[5] == "*" ->
                RecurringScheduleResponse(
                    mode = RecurringScheduleMode.DAILY,
                    timeOfDay = formatTimeOfDay(parts[2], parts[1])
                )

            parts[3] == "*" && parts[4] == "*" ->
                RecurringScheduleResponse(
                    mode = RecurringScheduleMode.WEEKLY,
                    timeOfDay = formatTimeOfDay(parts[2], parts[1]),
                    dayOfWeek = formatDayOfWeek(parts[5])
                )

            parts[4] == "*" && parts[5] == "*" ->
                RecurringScheduleResponse(
                    mode = RecurringScheduleMode.MONTHLY,
                    timeOfDay = formatTimeOfDay(parts[2], parts[1]),
                    dayOfMonth = parts[3].toIntOrNull()
                )

            else -> null
        }
    }

    private fun buildEveryCron(schedule: RecurringScheduleRequest): String {
        val interval = schedule.interval ?: throw ValidationException("recurringSchedule.interval is required when mode is EVERY")
        if (interval <= 0) throw ValidationException("recurringSchedule.interval must be greater than 0")

        return when (schedule.intervalUnit ?: throw ValidationException("recurringSchedule.intervalUnit is required when mode is EVERY")) {
            RecurringIntervalUnit.MINUTES -> {
                if (interval > 59) throw ValidationException("recurringSchedule.interval must be between 1 and 59 for MINUTES")
                "0 */$interval * * * *"
            }
            RecurringIntervalUnit.HOURS -> {
                if (interval > 23) throw ValidationException("recurringSchedule.interval must be between 1 and 23 for HOURS")
                "0 0 */$interval * * *"
            }
        }
    }

    private fun requireTimeOfDay(value: String?): LocalTime {
        if (value.isNullOrBlank()) throw ValidationException("recurringSchedule.timeOfDay is required")
        return try {
            LocalTime.parse(value).withSecond(0).withNano(0)
        } catch (_: Exception) {
            throw ValidationException("recurringSchedule.timeOfDay must use HH:mm format")
        }
    }

    private fun requireDayOfWeek(value: String?): DayOfWeek {
        if (value.isNullOrBlank()) throw ValidationException("recurringSchedule.dayOfWeek is required")
        return try {
            DayOfWeek.valueOf(value.trim().uppercase())
        } catch (_: Exception) {
            throw ValidationException("recurringSchedule.dayOfWeek must be a valid weekday name")
        }
    }

    private fun requireDayOfMonth(value: Int?): Int {
        val day = value ?: throw ValidationException("recurringSchedule.dayOfMonth is required")
        if (day !in 1..31) throw ValidationException("recurringSchedule.dayOfMonth must be between 1 and 31")
        return day
    }

    private fun formatTimeOfDay(hour: String, minute: String): String? {
        val parsedHour = hour.toIntOrNull() ?: return null
        val parsedMinute = minute.toIntOrNull() ?: return null
        return "%02d:%02d".format(parsedHour, parsedMinute)
    }

    private fun formatDayOfWeek(value: String): String? {
        val day = value.toIntOrNull() ?: return null
        return when (day) {
            0, 7 -> DayOfWeek.SUNDAY.name
            in 1..6 -> DayOfWeek.of(day).name
            else -> null
        }
    }
}
