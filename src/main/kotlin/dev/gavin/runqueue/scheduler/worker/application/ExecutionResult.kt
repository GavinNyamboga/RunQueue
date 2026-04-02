package dev.gavin.runqueue.scheduler.worker.application

sealed interface ExecutionResult {
    val success: Boolean
}

data class ExecutionSuccess(
    val output: Map<String, Any?> = emptyMap()
) : ExecutionResult {
    override val success: Boolean = true
}

data class ExecutionFailure(
    val errorCode: String? = null,
    val message: String
) : ExecutionResult {
    override val success: Boolean = false
}