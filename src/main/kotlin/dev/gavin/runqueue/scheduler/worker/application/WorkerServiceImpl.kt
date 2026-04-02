package dev.gavin.runqueue.scheduler.worker.application

import com.fasterxml.jackson.databind.ObjectMapper
import dev.gavin.runqueue.jobs.domain.RetryStrategy
import dev.gavin.runqueue.jobs.infrastructure.JobRepository
import dev.gavin.runqueue.runs.application.ExecutionLogWriter
import dev.gavin.runqueue.runs.domain.ExecutionLog
import dev.gavin.runqueue.runs.domain.JobRun
import dev.gavin.runqueue.runs.domain.JobRunStatus
import dev.gavin.runqueue.runs.infrastructure.JobRunRepository
import dev.gavin.runqueue.scheduler.worker.domain.WorkerHeartbeat
import dev.gavin.runqueue.scheduler.worker.domain.WorkerStatus
import dev.gavin.runqueue.scheduler.worker.infrastructure.WorkerHeartbeatRepository
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class WorkerServiceImpl(
    private val jobRunRepository: JobRunRepository,
    private val jobRepository: JobRepository,
    private val executionLogWriter: ExecutionLogWriter,
    private val workerHeartbeatRepository: WorkerHeartbeatRepository,
    private val executors: List<JobExecutor>,
    private val objectMapper: ObjectMapper,
    @Value("\${worker.name:runqueue-worker}")
    private val workerName: String,
    @Value("\${worker.stale-timeout-ms:30000}")
    private val staleTimeoutMs: Long
) : WorkerService {

    private val workerId = "worker-${UUID.randomUUID()}"

    @Scheduled(fixedDelayString = "\${worker.heartbeat-delay-ms:5000}")
    override fun recordHeartbeat() {
        val heartbeat = workerHeartbeatRepository.findById(workerId).orElse(
            WorkerHeartbeat(
                workerId = workerId,
                workerName = workerName
            )
        )

        if (heartbeat != null) {
            heartbeat.workerName = workerName
            heartbeat.status = WorkerStatus.ONLINE
            heartbeat.lastHeartbeatAt = Instant.now()
        }

        workerHeartbeatRepository.save(heartbeat)
    }

    @Scheduled(fixedDelayString = "\${worker.poll-delay-ms:3000}")
    override fun processQueuedRUns() {
        recordHeartbeat()
        recoverStaleWorkers()
        requeueDueRetries()

        val queuedRuns = jobRunRepository.findTop5ByStatusOrderByCreatedAtAsc(JobRunStatus.QUEUED)

        queuedRuns.forEach { run ->
            val job = jobRepository.findById(run.jobId).orElse(null) ?: return@forEach
            val executor = executors.firstOrNull { it.supports(job.type) } ?: return@forEach

            run.status = JobRunStatus.RUNNING
            run.startedAt = Instant.now()
            run.workerId = workerId
            jobRunRepository.save(run)
            logRunEvent(
                run = run,
                level = "INFO",
                eventType = "RUN_STARTED",
                message = "Run started by worker $workerId",
                details = mapOf(
                    "jobType" to job.type.name,
                    "workerName" to workerName
                )
            )

            when (val result = executor.execute(job, run)) {
                is ExecutionSuccess -> {
                    run.status = JobRunStatus.SUCCESS
                    run.finishedAt = Instant.now()
                    run.resultJson = objectMapper.writeValueAsString(result.output)
                    jobRunRepository.save(run)
                    logRunEvent(
                        run = run,
                        level = "INFO",
                        eventType = "RUN_SUCCEEDED",
                        message = "Run completed successfully",
                        details = result.output
                    )
                }

                is ExecutionFailure -> {
                    run.finishedAt = Instant.now()
                    run.errorMessage = result.message

                    if (run.attempt <= job.maxRetries) {
                        run.status = JobRunStatus.RETRY_SCHEDULED
                        run.nextRetryAt = calculateRetryTime(job.retryStrategy, job.retryDelaySeconds, run.attempt)
                        logRunEvent(
                            run = run,
                            level = "WARN",
                            eventType = "RETRY_SCHEDULED",
                            message = result.message,
                            errorCode = result.errorCode,
                            details = mapOf(
                                "nextRetryAt" to run.nextRetryAt.toString(),
                                "retryStrategy" to job.retryStrategy.name,
                                "maxRetries" to job.maxRetries
                            )
                        )
                    } else {
                        run.status = JobRunStatus.FAILED
                        logRunEvent(
                            run = run,
                            level = "ERROR",
                            eventType = "RUN_FAILED",
                            message = result.message,
                            errorCode = result.errorCode,
                            details = mapOf(
                                "maxRetries" to job.maxRetries
                            )
                        )
                    }

                    jobRunRepository.save(run)
                }
            }
        }
    }

    @PreDestroy
    fun markOffline() {
        val heartbeat = workerHeartbeatRepository.findById(workerId).orElse(null) ?: return
        heartbeat.status = WorkerStatus.OFFLINE
        heartbeat.lastHeartbeatAt = Instant.now()
        workerHeartbeatRepository.save(heartbeat)
    }

    private fun requeueDueRetries() {
        val now = Instant.now()
        val retries = jobRunRepository.findAllByStatusAndNextRetryAtLessThanEqual(
            JobRunStatus.RETRY_SCHEDULED,
            now
        )

        retries.forEach { run ->
            run.status = JobRunStatus.QUEUED
            run.attempt += 1
            run.queuedAt = now
            run.startedAt = null
            run.finishedAt = null
            run.nextRetryAt = null
            jobRunRepository.save(run)
            logRunEvent(
                run = run,
                level = "INFO",
                eventType = "RETRY_QUEUED",
                message = "Retry queued for another execution attempt",
                details = mapOf(
                    "attempt" to run.attempt
                )
            )
        }
    }

    private fun recoverStaleWorkers() {
        val cutoff = Instant.now().minusMillis(staleTimeoutMs)
        val staleWorkers = workerHeartbeatRepository.findAllByStatusAndLastHeartbeatAtBefore(
            WorkerStatus.ONLINE,
            cutoff
        )

        if (staleWorkers.isEmpty()) return

        staleWorkers.forEach { heartbeat ->
            heartbeat.status = WorkerStatus.STALE
            workerHeartbeatRepository.save(heartbeat)
        }

        val staleWorkerIds = staleWorkers.map { it.workerId }
        val abandonedRuns = jobRunRepository.findAllByStatusAndWorkerIdIn(JobRunStatus.RUNNING, staleWorkerIds)
        val now = Instant.now()

        abandonedRuns.forEach { run ->
            requeueRunFromStaleWorker(run, now)
        }
    }

    private fun calculateRetryTime(
        strategy: RetryStrategy,
        delaySeconds: Long,
        attempt: Int
    ): Instant {
        val seconds = when (strategy) {
            RetryStrategy.NONE -> delaySeconds
            RetryStrategy.FIXED_DELAY -> delaySeconds
            RetryStrategy.EXPONENTIAL_BACKOFF -> delaySeconds * (1L shl (attempt - 1))
        }
        return Instant.now().plusSeconds(seconds)
    }

    private fun requeueRunFromStaleWorker(run: JobRun, now: Instant) {
        val previousWorkerId = run.workerId

        run.status = JobRunStatus.QUEUED
        run.queuedAt = now
        run.startedAt = null
        run.workerId = null
        run.errorMessage = previousWorkerId?.let { "Recovered from stale worker $it" }
        jobRunRepository.save(run)
        logRunEvent(
            run = run,
            level = "WARN",
            eventType = "RUN_RECOVERED",
            message = run.errorMessage ?: "Recovered run from stale worker",
            details = mapOf(
                "recoveredAt" to now.toString(),
                "previousWorkerId" to previousWorkerId
            )
        )
    }

    private fun logRunEvent(
        run: JobRun,
        level: String,
        eventType: String,
        message: String,
        errorCode: String? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        val detailsJson = if (details.isEmpty()) null else objectMapper.writeValueAsString(details)
        executionLogWriter.persistAsync(
            ExecutionLog(
                runId = run.id,
                jobId = run.jobId,
                workerId = run.workerId,
                attempt = run.attempt,
                eventType = eventType,
                runStatus = run.status.name,
                level = level,
                message = message,
                errorCode = errorCode,
                detailsJson = detailsJson
            )
        )
    }
}
