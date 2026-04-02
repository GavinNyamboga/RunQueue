package dev.gavin.runqueue.runs.infrastructure

import dev.gavin.runqueue.runs.domain.JobRun
import dev.gavin.runqueue.runs.domain.JobRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface JobRunRepository : JpaRepository<JobRun, UUID> {

    fun findAllByJobIdOrderByCreatedAtDesc(jobId: UUID): List<JobRun>

    fun findTop5ByStatusOrderByCreatedAtAsc(status: JobRunStatus): List<JobRun>

    fun findAllByStatusAndNextRetryAtLessThanEqual(status: JobRunStatus, time: Instant): List<JobRun>

    fun existsByJobIdAndStatusIn(jobId: UUID, statuses: Collection<JobRunStatus>): Boolean

    fun findAllByStatusAndWorkerIdIn(status: JobRunStatus, workerIds: Collection<String>): List<JobRun>

}
