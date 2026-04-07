package dev.gavin.runqueue.jobs.api

import dev.gavin.runqueue.jobs.domain.JobType
import dev.gavin.runqueue.jobs.domain.RetryStrategy
import dev.gavin.runqueue.jobs.domain.ScheduleType
import jakarta.validation.constraints.*
import java.time.Instant

data class CreateJobRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:Size(max = 500)
    val description: String? = null,

    var type: JobType = JobType.HTTP,

    @field:NotNull
    var scheduleType: ScheduleType,

    val cronExpression: String? = null,

    val recurringSchedule: RecurringScheduleRequest? = null,

    val runAt: Instant? = null,

    val payload: Map<String, Any>? = null,

    val retryStrategy: RetryStrategy = RetryStrategy.NONE,

    @field:Min(0)
    @field:Max(20)
    val maxRetries: Int = 0,

    @field:Min(0)
    val retryDelaySeconds: Long = 0,

    @field:Min(1)
    val timeoutSeconds: Long = 60

)
