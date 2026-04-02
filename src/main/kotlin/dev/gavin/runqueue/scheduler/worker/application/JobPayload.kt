package dev.gavin.runqueue.scheduler.worker.application

sealed interface JobPayload

data class HttpJobPayload(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null
) : JobPayload

data class MockJobPayload(
    val durationMillis: Long = 1000,
    val shouldFail: Boolean = false,
    val failureMessage: String? = null,
) : JobPayload