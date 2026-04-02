package dev.gavin.runqueue.runs.application

import dev.gavin.runqueue.runs.api.JobRunResponse
import dev.gavin.runqueue.runs.domain.JobRun

fun JobRun.toResponse(): JobRunResponse =
    JobRunResponse(
        id = id,
        jobId = jobId,
        status = status,
        attempt = attempt,
        scheduledAt = scheduledAt,
        queuedAt = queuedAt,
        startedAt = startedAt,
        finishedAt = finishedAt,
        nextRetryAt = nextRetryAt,
        workerId = workerId,
        errorMessage = errorMessage,
        resultJson = resultJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )