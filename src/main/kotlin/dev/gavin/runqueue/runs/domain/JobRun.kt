package dev.gavin.runqueue.runs.domain

import dev.gavin.runqueue.common.domain.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "job_runs",
    indexes = [
        Index(name = "idx_job_runs_job_id", columnList = "job_id"),
        Index(name = "idx_job_runs_status", columnList = "status"),
        Index(name = "idx_job_runs_scheduled_at", columnList = "scheduledAt")
    ]
)
class JobRun(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var jobId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: JobRunStatus = JobRunStatus.PENDING,

    @Column(nullable = false)
    var attempt: Int = 1,

    @Column(nullable = false)
    var scheduledAt: Instant,

    @Column
    var queuedAt: Instant? = null,

    @Column
    var startedAt: Instant? = null,

    @Column
    var finishedAt: Instant? = null,

    @Column
    var nextRetryAt: Instant? = null,

    @Column
    var workerId: String? = null,

    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    var resultJson: String? = null,

    @Version
    var version: Long? = null
) : BaseEntity()