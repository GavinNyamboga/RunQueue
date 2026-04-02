package dev.gavin.runqueue.runs.api

import dev.gavin.runqueue.runs.domain.JobRunStatus
import java.time.Instant
import java.util.*

data class JobRunResponse(
    val id: UUID,
    val jobId: UUID,
    val status: JobRunStatus,
    val attempt: Int,
    val scheduledAt: Instant,
    val queuedAt: Instant?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val nextRetryAt: Instant?,
    val workerId: String?,
    val errorMessage: String?,
    val resultJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)