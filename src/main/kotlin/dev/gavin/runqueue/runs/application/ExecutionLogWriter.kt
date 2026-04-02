package dev.gavin.runqueue.runs.application

import dev.gavin.runqueue.runs.domain.ExecutionLog
import dev.gavin.runqueue.runs.infrastructure.ExecutionLogRepository
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ExecutionLogWriter(
    private val persistenceService: ExecutionLogPersistenceService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun persistAsync(log: ExecutionLog) {
        scope.launch {
            persistenceService.persist(log)
        }
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }
}

@Service
class ExecutionLogPersistenceService(
    private val executionLogRepository: ExecutionLogRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun persist(log: ExecutionLog) {
        executionLogRepository.save(log)
    }
}
