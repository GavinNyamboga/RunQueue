package dev.gavin.runqueue.jobs.domain

import dev.gavin.runqueue.common.domain.BaseEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "jobs")
class Job(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 120)
    var name: String,

    @Column(columnDefinition = "TEXT")
    @Lob
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: JobType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var scheduleType: ScheduleType,

    @Column
    var cronExpression: String? = null,

    @Column
    var runAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: JobStatus = JobStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var retryStrategy: RetryStrategy = RetryStrategy.NONE,

    @Column(nullable = false)
    var maxRetries: Int = 0,

    @Column(nullable = false)
    var retryDelaySeconds: Long = 0,

    @Column(nullable = false)
    var timeoutSeconds: Long = 60,

    @Lob
    @Column(columnDefinition = "TEXT")
    var payloadJson: String? = null,

    @Column
    var lastScheduledAt: Instant? = null,

    @Column
    var nextRunAt: Instant? = null,

    @Version
    var version: Long? = null

) : BaseEntity()
