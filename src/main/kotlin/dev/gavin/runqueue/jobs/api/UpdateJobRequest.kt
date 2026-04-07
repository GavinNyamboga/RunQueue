package dev.gavin.runqueue.jobs.api

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant

data class UpdateJobRequest(
    val description: String?,
    val cronExpression: String?,
    val recurringSchedule: RecurringScheduleRequest?,
    val runAt: Instant?,
    val payload: Map<String, Any?>?,
    @field:Min(0)
    @field:Max(20)
    val maxRetries: Int?,
    val retryDelaySeconds: Long?,
    val timeoutSeconds: Long?
)
