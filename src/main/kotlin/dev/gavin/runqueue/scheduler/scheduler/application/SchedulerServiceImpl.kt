package dev.gavin.runqueue.scheduler.scheduler.application

import dev.gavin.runqueue.jobs.application.ScheduleCalculator
import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.infrastructure.JobRepository
import dev.gavin.runqueue.runs.domain.JobRun
import dev.gavin.runqueue.runs.domain.JobRunStatus
import dev.gavin.runqueue.runs.infrastructure.JobRunRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class SchedulerServiceImpl(
    private val jobRepository: JobRepository,
    private val jobRunRepository: JobRunRepository,
    private val scheduleCalculator: ScheduleCalculator
) : SchedulerService {

    @Scheduled(fixedRateString = $$"${scheduler.scan-delay-ms:5000}")
    override fun scheduleDueJobs() {
        val now = Instant.now()
        val dueJobs = jobRepository.findDueJobs(now)

        dueJobs.forEach { scheduleJobIfStillDue(it, now) }
    }

    override fun scheduleJobIfDue(jobId: UUID): Boolean {
        val job = jobRepository.findById(jobId).orElse(null) ?: return false
        return scheduleJobIfStillDue(job, Instant.now())
    }

    private fun scheduleJobIfStillDue(job: Job, now: Instant): Boolean {
        val nextRunAt = job.nextRunAt ?: return false
        if (nextRunAt.isAfter(now)) return false

        if (jobRunRepository.existsByJobIdAndStatusIn(job.id, ACTIVE_RUN_STATUSES)) {
            return false
        }

        val run = JobRun(
            jobId = job.id,
            status = JobRunStatus.QUEUED,
            attempt = 1,
            scheduledAt = now,
            queuedAt = now
        )
        jobRunRepository.save(run)

        job.lastScheduledAt = now
        job.nextRunAt = scheduleCalculator.calculateNextRun(
            scheduleType = job.scheduleType,
            cronExpression = job.cronExpression,
            lastRunAt = now
        )

        jobRepository.save(job)
        return true
    }

    private companion object {
        val ACTIVE_RUN_STATUSES = listOf(
            JobRunStatus.QUEUED,
            JobRunStatus.RUNNING,
            JobRunStatus.RETRY_SCHEDULED
        )
    }
}
