package dev.gavin.runqueue.jobs.application

import com.fasterxml.jackson.databind.ObjectMapper
import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.domain.JobStatus
import dev.gavin.runqueue.jobs.domain.JobType
import dev.gavin.runqueue.jobs.domain.RetryStrategy
import dev.gavin.runqueue.jobs.domain.ScheduleType
import dev.gavin.runqueue.jobs.infrastructure.JobRepository
import dev.gavin.runqueue.scheduler.scheduler.application.SchedulerService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals

class JobServiceImplTest {
    private val jobRepository = mock(JobRepository::class.java)
    private val scheduleCalculator = mock(ScheduleCalculator::class.java)
    private val objectMapper = ObjectMapper()
    private val schedulerService = mock(SchedulerService::class.java)

    private val service = JobServiceImpl(
        jobRepository = jobRepository,
        scheduleCalculator = scheduleCalculator,
        objectMapper = objectMapper,
        schedulerService = schedulerService
    )

    @Test
    fun `resume schedules overdue jobs immediately`() {
        val job = overduePausedJob()
        given(jobRepository.findById(job.id)).willReturn(Optional.of(job))
        given(scheduleCalculator.calculateFirstRun(job.scheduleType, job.cronExpression, job.runAt)).willReturn(job.runAt)
        given(jobRepository.save(job)).willReturn(job)

        val response = service.resumeJob(job.id)

        assertEquals(JobStatus.ACTIVE, response.status)
        verify(schedulerService).scheduleJobIfDue(job.id)
    }

    @Test
    fun `resume does not trigger scheduling for future jobs`() {
        val futureRunAt = Instant.now().plusSeconds(300)
        val job = overduePausedJob(runAt = futureRunAt)
        given(jobRepository.findById(job.id)).willReturn(Optional.of(job))
        given(scheduleCalculator.calculateFirstRun(job.scheduleType, job.cronExpression, job.runAt)).willReturn(futureRunAt)
        given(jobRepository.save(job)).willReturn(job)

        val response = service.resumeJob(job.id)

        assertEquals(futureRunAt, response.nextRunAt)
        verify(schedulerService, never()).scheduleJobIfDue(job.id)
    }

    private fun overduePausedJob(runAt: Instant = Instant.now().minusSeconds(60)): Job =
        Job(
            name = "resume-job-${runAt.epochSecond}",
            type = JobType.HTTP,
            scheduleType = ScheduleType.ONCE,
            runAt = runAt,
            status = JobStatus.PAUSED,
            retryStrategy = RetryStrategy.NONE,
            maxRetries = 0,
            retryDelaySeconds = 0,
            timeoutSeconds = 60,
            nextRunAt = runAt
        )
}
