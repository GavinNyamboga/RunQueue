package dev.gavin.runqueue.jobs.domain

enum class RetryStrategy {
    NONE,
    FIXED_DELAY,
    EXPONENTIAL_BACKOFF
}