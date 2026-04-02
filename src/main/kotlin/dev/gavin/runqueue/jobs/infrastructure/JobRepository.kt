package dev.gavin.runqueue.jobs.infrastructure

import dev.gavin.runqueue.jobs.domain.Job
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface JobRepository : JpaRepository<Job, UUID> {

    @Query(
        """
        select j from Job j
        where j.status = 'ACTIVE'
          and j.nextRunAt is not null
          and j.nextRunAt <= :now
    """
    )
    fun findDueJobs(now: Instant): List<Job>
}