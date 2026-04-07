package dev.gavin.runqueue.jobs.api

import dev.gavin.runqueue.jobs.domain.JobStatus
import dev.gavin.runqueue.jobs.domain.JobType
import dev.gavin.runqueue.jobs.domain.RetryStrategy
import dev.gavin.runqueue.jobs.domain.ScheduleType
import java.time.Instant
import java.util.*

data class JobResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val type: JobType,
    val scheduleType: ScheduleType,
    val cronExpression: String?,
    val recurringSchedule: RecurringScheduleResponse?,
    val runAt: Instant?,
    val status: JobStatus,
    val retryStrategy: RetryStrategy,
    val maxRetries: Int,
    val retryDelaySeconds: Long,
    val timeoutSeconds: Long,
    val lastScheduledAt: Instant?,
    val nextRunAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
