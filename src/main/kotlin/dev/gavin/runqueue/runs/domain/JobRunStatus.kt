package dev.gavin.runqueue.runs.domain

enum class JobRunStatus {
    PENDING,
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRY_SCHEDULED,
    CANCELLED
}