package dev.gavin.runqueue.scheduler.worker.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "worker_heartbeats")
class WorkerHeartbeat(
    @Id
    @Column(nullable = false)
    var workerId: String,

    @Column(nullable = false, length = 120)
    var workerName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WorkerStatus = WorkerStatus.ONLINE,

    @Column(nullable = false)
    var lastHeartbeatAt: Instant = Instant.now()

)