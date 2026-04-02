package dev.gavin.runqueue.runs.infrastructure

import dev.gavin.runqueue.runs.domain.ExecutionLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExecutionLogRepository : JpaRepository<ExecutionLog, UUID> {
    fun findAllByRunIdOrderByLoggedAtAsc(runId: UUID): List<ExecutionLog>
}
