package dev.gavin.runqueue.runs.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "execution_logs")
class ExecutionLog(

    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var runId: UUID,

    @Column(nullable = false)
    var jobId: UUID,

    @Column
    var workerId: String? = null,

    @Column(nullable = false)
    var attempt: Int = 1,

    @Column(nullable = false, length = 40)
    var eventType: String,

    @Column(nullable = false, length = 30)
    var runStatus: String,

    @Column(nullable = false, length = 20)
    var level: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var message: String,

    @Column(length = 100)
    var errorCode: String? = null,

    @Column(columnDefinition = "TEXT")
    var detailsJson: String? = null,

    @Column(nullable = false)
    var loggedAt: Instant = Instant.now()
)
