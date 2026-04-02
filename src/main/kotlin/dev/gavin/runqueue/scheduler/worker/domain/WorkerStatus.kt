package dev.gavin.runqueue.scheduler.worker.domain

enum class WorkerStatus {
    ONLINE,
    OFFLINE,
    STALE
}