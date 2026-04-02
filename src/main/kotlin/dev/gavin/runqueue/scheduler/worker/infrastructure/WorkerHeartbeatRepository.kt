package dev.gavin.runqueue.scheduler.worker.infrastructure

import dev.gavin.runqueue.scheduler.worker.domain.WorkerHeartbeat
import dev.gavin.runqueue.scheduler.worker.domain.WorkerStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface WorkerHeartbeatRepository : JpaRepository<WorkerHeartbeat, String> {
    fun findAllByStatusAndLastHeartbeatAtBefore(status: WorkerStatus, cutoff: Instant): List<WorkerHeartbeat>
}
