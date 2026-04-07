package dev.gavin.runqueue.jobs.application

import com.fasterxml.jackson.databind.ObjectMapper
import dev.gavin.runqueue.common.api.NotFoundException
import dev.gavin.runqueue.common.api.ValidationException
import dev.gavin.runqueue.jobs.api.CreateJobRequest
import dev.gavin.runqueue.jobs.api.JobResponse
import dev.gavin.runqueue.jobs.api.UpdateJobRequest
import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.domain.JobStatus
import dev.gavin.runqueue.jobs.domain.ScheduleType
import dev.gavin.runqueue.jobs.infrastructure.JobRepository
import dev.gavin.runqueue.scheduler.scheduler.application.SchedulerService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class JobServiceImpl(
    private val jobRepository: JobRepository,
    private val scheduleCalculator: ScheduleCalculator,
    private val objectMapper: ObjectMapper,
    private val schedulerService: SchedulerService
) : JobService {
    override fun createJob(request: CreateJobRequest): JobResponse {
        val cronExpression = resolveCronExpression(request.scheduleType, request.cronExpression, request.recurringSchedule)
        validateSchedule(request.scheduleType, cronExpression, request.runAt)

        val job = Job(
            name = request.name,
            description = request.description,
            type = request.type,
            scheduleType = request.scheduleType,
            cronExpression = cronExpression,
            runAt = request.runAt,
            retryStrategy = request.retryStrategy,
            maxRetries = request.maxRetries,
            retryDelaySeconds = request.retryDelaySeconds,
            timeoutSeconds = request.timeoutSeconds,
            payloadJson = request.payload?.let { objectMapper.writeValueAsString(it) },
            nextRunAt = scheduleCalculator.calculateFirstRun(
                request.scheduleType,
                cronExpression,
                request.runAt,
            )
        )

        val savedJob = jobRepository.save(job)
        triggerSchedulingIfDue(savedJob)
        return savedJob.toResponse()
    }

    @Transactional(readOnly = true)
    override fun getById(id: UUID): JobResponse {
        val job = jobRepository.findById(id).orElseThrow { NotFoundException("Job not found with id: $id") }
        return job.toResponse()
    }

    @Transactional(readOnly = true)
    override fun listJobsPaged(pageable: Pageable): Page<JobResponse> =
        jobRepository.findAll(pageable)
            .map { it.toResponse() }

    override fun updateJob(id: UUID, request: UpdateJobRequest): JobResponse {
        val job = jobRepository.findById(id).orElseThrow { NotFoundException("Job not found with id: $id") }

        request.description?.let { job.description = it }
        val cronExpression = resolveUpdatedCronExpression(job.cronExpression, request.cronExpression, request.recurringSchedule)
        job.cronExpression = cronExpression
        request.runAt?.let { job.runAt = it }
        request.payload?.let { job.payloadJson = objectMapper.writeValueAsString(it) }
        request.maxRetries?.let { job.maxRetries = it }
        request.retryDelaySeconds?.let { job.retryDelaySeconds = it }
        request.timeoutSeconds?.let { job.timeoutSeconds = it }

        validateSchedule(job.scheduleType, job.cronExpression, job.runAt)
        job.nextRunAt = when (job.status) {
            JobStatus.DELETED -> null
            else -> scheduleCalculator.calculateFirstRun(job.scheduleType, job.cronExpression, job.runAt)
        }

        val updatedJob = jobRepository.save(job)
        triggerSchedulingIfDue(updatedJob)
        return updatedJob.toResponse()
    }

    override fun pauseJob(id: UUID): JobResponse {
        val job = jobRepository.findById(id).orElseThrow { NotFoundException("Job not found with id: $id") }

        job.status = JobStatus.PAUSED
        val resumedJob = jobRepository.save(job)
        triggerSchedulingIfDue(resumedJob)
        return resumedJob.toResponse()
    }

    override fun resumeJob(id: UUID): JobResponse {
        val job = jobRepository.findById(id).orElseThrow { NotFoundException("Job not found with id: $id") }

        job.status = JobStatus.ACTIVE
        job.nextRunAt = scheduleCalculator.calculateFirstRun(
            job.scheduleType,
            job.cronExpression,
            job.runAt,
        )
        return jobRepository.save(job).toResponse()
    }

    override fun deleteJob(id: UUID) {
        val job = jobRepository.findById(id).orElseThrow { NotFoundException("Job not found with id: $id") }

        job.status = JobStatus.DELETED
        job.nextRunAt = null
        jobRepository.save(job)
    }

    private fun validateSchedule(
        scheduleType: ScheduleType,
        cronExpression: String?,
        runAt: Instant?
    ) {
        when (scheduleType) {
            ScheduleType.ONCE -> {
                if (runAt == null) throw ValidationException("runAt is required for ONCE jobs")
                if (!cronExpression.isNullOrBlank()) throw ValidationException("cronExpression is not supported for ONCE jobs")
            }

            ScheduleType.CRON -> {
                if (cronExpression.isNullOrEmpty()) throw ValidationException("cronExpression or recurringSchedule is required for CRON jobs")
                if (runAt != null) throw ValidationException("runAt is not supported for CRON jobs")
            }
        }
    }

    private fun resolveCronExpression(
        scheduleType: ScheduleType,
        cronExpression: String?,
        recurringSchedule: dev.gavin.runqueue.jobs.api.RecurringScheduleRequest?
    ): String? {
        if (scheduleType != ScheduleType.CRON) return cronExpression
        if (!cronExpression.isNullOrBlank() && recurringSchedule != null) {
            throw ValidationException("Provide either cronExpression or recurringSchedule, not both")
        }
        return recurringSchedule?.let(RecurringScheduleMapper::toCronExpression) ?: cronExpression
    }

    private fun resolveUpdatedCronExpression(
        existingCronExpression: String?,
        cronExpression: String?,
        recurringSchedule: dev.gavin.runqueue.jobs.api.RecurringScheduleRequest?
    ): String? {
        if (!cronExpression.isNullOrBlank() && recurringSchedule != null) {
            throw ValidationException("Provide either cronExpression or recurringSchedule, not both")
        }
        return when {
            recurringSchedule != null -> RecurringScheduleMapper.toCronExpression(recurringSchedule)
            !cronExpression.isNullOrBlank() -> cronExpression
            else -> existingCronExpression
        }
    }

    private fun triggerSchedulingIfDue(job: Job) {
        val nextRunAt = job.nextRunAt ?: return
        if (job.status == JobStatus.ACTIVE && !nextRunAt.isAfter(Instant.now())) {
            schedulerService.scheduleJobIfDue(job.id)
        }
    }
}
