package dev.gavin.runqueue.jobs.application

import dev.gavin.runqueue.jobs.api.JobResponse
import dev.gavin.runqueue.jobs.domain.Job

fun Job.toResponse(): JobResponse =
    JobResponse(
        id = id,
        name = name,
        description = description,
        type = type,
        scheduleType = scheduleType,
        cronExpression = cronExpression,
        runAt = runAt,
        status = status,
        retryStrategy = retryStrategy,
        maxRetries = maxRetries,
        retryDelaySeconds = retryDelaySeconds,
        timeoutSeconds = timeoutSeconds,
        lastScheduledAt = lastScheduledAt,
        nextRunAt = nextRunAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )