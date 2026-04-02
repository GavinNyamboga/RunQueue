package dev.gavin.runqueue

import dev.gavin.runqueue.jobs.domain.*
import dev.gavin.runqueue.jobs.infrastructure.JobRepository
import dev.gavin.runqueue.runs.domain.JobRun
import dev.gavin.runqueue.runs.domain.JobRunStatus
import dev.gavin.runqueue.runs.infrastructure.ExecutionLogRepository
import dev.gavin.runqueue.runs.infrastructure.JobRunRepository
import dev.gavin.runqueue.scheduler.scheduler.application.SchedulerService
import dev.gavin.runqueue.scheduler.worker.application.WorkerService
import dev.gavin.runqueue.scheduler.worker.domain.WorkerHeartbeat
import dev.gavin.runqueue.scheduler.worker.domain.WorkerStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class SchedulerWorkerIntegrationTest : ApiIntegrationTest() {
    @Autowired
    private lateinit var schedulerService: SchedulerService

    @Autowired
    private lateinit var workerService: WorkerService

    @Autowired
    private lateinit var localJobRepository: JobRepository

    @Autowired
    private lateinit var localJobRunRepository: JobRunRepository

    @Autowired
    private lateinit var localExecutionLogRepository: ExecutionLogRepository

    @Test
    fun `creating a due job queues a run immediately`() {
        val pastRunAt = Instant.now().minusSeconds(30)

        val response =
            request()
                .contentType("application/json")
                .body(
                    """
                    {
                      "name": "run-immediately",
                      "type": "HTTP",
                      "scheduleType": "ONCE",
                      "runAt": "$pastRunAt",
                      "payload": {
                        "method": "POST",
                        "url": "https://example.com/hook"
                      }
                    }
                    """.trimIndent()
                )
                .post("/api/jobs")
                .then()
                .statusCode(201)
                .extract()

        val jobId = java.util.UUID.fromString(response.path("id"))
        val runs = localJobRunRepository.findAllByJobIdOrderByCreatedAtDesc(jobId)

        kotlin.test.assertEquals(1, runs.size)
        kotlin.test.assertEquals(JobRunStatus.QUEUED, runs.single().status)
    }

    @Test
    fun `scheduler does not create duplicate queued runs for the same due job`() {
        val job = localJobRepository.save(
            Job(
                name = "already-queued",
                type = JobType.HTTP,
                scheduleType = ScheduleType.ONCE,
                runAt = Instant.now().minusSeconds(60),
                status = JobStatus.ACTIVE,
                retryStrategy = RetryStrategy.NONE,
                maxRetries = 0,
                retryDelaySeconds = 0,
                timeoutSeconds = 60,
                payloadJson = """{"method":"POST","url":"https://example.com/hook"}""",
                nextRunAt = Instant.now().minusSeconds(60),
            )
        )

        localJobRunRepository.save(
            JobRun(
                jobId = job.id,
                status = JobRunStatus.QUEUED,
                attempt = 1,
                scheduledAt = Instant.now().minusSeconds(60),
                queuedAt = Instant.now().minusSeconds(60),
            )
        )

        schedulerService.scheduleDueJobs()

        val runs = localJobRunRepository.findAllByJobIdOrderByCreatedAtDesc(job.id)
        kotlin.test.assertEquals(1, runs.size)
    }

    @Test
    fun `worker requeues due retries before polling queued work`() {
        val job = localJobRepository.save(
            Job(
                name = "retry-now",
                type = JobType.MOCK,
                scheduleType = ScheduleType.ONCE,
                runAt = Instant.now().minusSeconds(60),
                status = JobStatus.ACTIVE,
                retryStrategy = RetryStrategy.FIXED_DELAY,
                maxRetries = 1,
                retryDelaySeconds = 0,
                timeoutSeconds = 60,
                payloadJson = """{"durationMillis":0,"shouldFail":false}""",
                nextRunAt = null,
            )
        )

        val retryRun = localJobRunRepository.save(
            JobRun(
                jobId = job.id,
                status = JobRunStatus.RETRY_SCHEDULED,
                attempt = 1,
                scheduledAt = Instant.now().minusSeconds(30),
                nextRetryAt = Instant.now().minusSeconds(1),
            )
        )

        workerService.processQueuedRUns()

        val persisted = localJobRunRepository.findById(retryRun.id).orElseThrow()
        val logs = waitForExecutionLogs(retryRun.id) { it.size >= 3 }

        kotlin.test.assertEquals(JobRunStatus.SUCCESS, persisted.status)
        kotlin.test.assertNotNull(persisted.startedAt)
        kotlin.test.assertNotNull(persisted.finishedAt)
        kotlin.test.assertNotNull(persisted.resultJson)
        kotlin.test.assertEquals(listOf("RETRY_QUEUED", "RUN_STARTED", "RUN_SUCCEEDED"), logs.map { it.eventType })
        kotlin.test.assertEquals(2, logs.first().attempt)
        kotlin.test.assertEquals(retryRun.jobId, logs.first().jobId)
    }

    @Test
    fun `automatic retry increments attempt before requeueing`() {
        val job = localJobRepository.save(
            Job(
                name = "retry-increments-attempt",
                type = JobType.MOCK,
                scheduleType = ScheduleType.ONCE,
                runAt = Instant.now().minusSeconds(60),
                status = JobStatus.ACTIVE,
                retryStrategy = RetryStrategy.FIXED_DELAY,
                maxRetries = 3,
                retryDelaySeconds = 0,
                timeoutSeconds = 60,
                payloadJson = """{"durationMillis":0,"shouldFail":false}""",
                nextRunAt = null,
            )
        )

        val retryRun = localJobRunRepository.save(
            JobRun(
                jobId = job.id,
                status = JobRunStatus.RETRY_SCHEDULED,
                attempt = 2,
                scheduledAt = Instant.now().minusSeconds(30),
                queuedAt = Instant.now().minusSeconds(30),
                startedAt = Instant.now().minusSeconds(25),
                finishedAt = Instant.now().minusSeconds(20),
                nextRetryAt = Instant.now().minusSeconds(1),
            )
        )

        workerService.processQueuedRUns()

        val persisted = localJobRunRepository.findById(retryRun.id).orElseThrow()
        kotlin.test.assertEquals(JobRunStatus.SUCCESS, persisted.status)
        kotlin.test.assertEquals(3, persisted.attempt)
    }

    @Test
    fun `automatic retries stop at configured max retries`() {
        val job = localJobRepository.save(
            Job(
                name = "retry-stops-at-max",
                type = JobType.MOCK,
                scheduleType = ScheduleType.ONCE,
                runAt = Instant.now().minusSeconds(60),
                status = JobStatus.ACTIVE,
                retryStrategy = RetryStrategy.FIXED_DELAY,
                maxRetries = 3,
                retryDelaySeconds = 0,
                timeoutSeconds = 60,
                payloadJson = """{"durationMillis":0,"shouldFail":true,"failureMessage":"boom"}""",
                nextRunAt = null,
            )
        )

        val run = localJobRunRepository.save(
            JobRun(
                jobId = job.id,
                status = JobRunStatus.QUEUED,
                attempt = 1,
                scheduledAt = Instant.now().minusSeconds(30),
                queuedAt = Instant.now().minusSeconds(30),
            )
        )

        repeat(4) {
            workerService.processQueuedRUns()
        }

        val persisted = localJobRunRepository.findById(run.id).orElseThrow()
        val logs = waitForExecutionLogs(run.id) { entries ->
            entries.any { it.eventType == "RUN_FAILED" && it.attempt == 4 }
        }

        kotlin.test.assertEquals(JobRunStatus.FAILED, persisted.status)
        kotlin.test.assertEquals(4, persisted.attempt)
        kotlin.test.assertEquals("boom", persisted.errorMessage)
        kotlin.test.assertTrue(logs.any { it.eventType == "RUN_FAILED" && it.attempt == 4 })
        kotlin.test.assertTrue(logs.any { it.eventType == "RETRY_SCHEDULED" && it.attempt == 3 })
    }

    @Test
    fun `record heartbeat upserts worker heartbeat`() {
        workerService.recordHeartbeat()

        val heartbeats = workerHeartbeatRepository.findAll()
        kotlin.test.assertEquals(1, heartbeats.size)
        kotlin.test.assertEquals(WorkerStatus.ONLINE, heartbeats.single().status)
        kotlin.test.assertTrue(heartbeats.single().workerName.isNotBlank())
    }

    @Test
    fun `worker recovers running jobs from stale workers`() {
        val job = localJobRepository.save(
            Job(
                name = "stale-worker-job",
                type = JobType.MOCK,
                scheduleType = ScheduleType.ONCE,
                runAt = Instant.now().minusSeconds(60),
                status = JobStatus.ACTIVE,
                retryStrategy = RetryStrategy.NONE,
                maxRetries = 0,
                retryDelaySeconds = 0,
                timeoutSeconds = 60,
                payloadJson = """{"durationMillis":0,"shouldFail":false}""",
                nextRunAt = null,
            )
        )

        workerHeartbeatRepository.save(
            WorkerHeartbeat(
                workerId = "stale-worker-1",
                workerName = "stale-worker",
                status = WorkerStatus.ONLINE,
                lastHeartbeatAt = Instant.now().minusSeconds(120)
            )
        )

        val abandonedRun = localJobRunRepository.save(
            JobRun(
                jobId = job.id,
                status = JobRunStatus.RUNNING,
                attempt = 1,
                scheduledAt = Instant.now().minusSeconds(30),
                queuedAt = Instant.now().minusSeconds(30),
                startedAt = Instant.now().minusSeconds(20),
                workerId = "stale-worker-1"
            )
        )

        workerService.processQueuedRUns()

        val recoveredRun = localJobRunRepository.findById(abandonedRun.id).orElseThrow()
        val staleHeartbeat = workerHeartbeatRepository.findById("stale-worker-1").orElseThrow()

        kotlin.test.assertEquals(JobRunStatus.SUCCESS, recoveredRun.status)
        kotlin.test.assertEquals(WorkerStatus.STALE, staleHeartbeat.status)
        kotlin.test.assertNotNull(recoveredRun.workerId)
        kotlin.test.assertEquals("Recovered from stale worker stale-worker-1", recoveredRun.errorMessage)
    }

    private fun waitForExecutionLogs(
        runId: java.util.UUID,
        timeoutMillis: Long = 2_000,
        condition: (List<dev.gavin.runqueue.runs.domain.ExecutionLog>) -> Boolean
    ): List<dev.gavin.runqueue.runs.domain.ExecutionLog> {
        val deadline = System.currentTimeMillis() + timeoutMillis

        while (System.currentTimeMillis() < deadline) {
            val logs = localExecutionLogRepository.findAllByRunIdOrderByLoggedAtAsc(runId)
            if (condition(logs)) {
                return logs
            }
            Thread.sleep(25)
        }

        return localExecutionLogRepository.findAllByRunIdOrderByLoggedAtAsc(runId)
    }
}
