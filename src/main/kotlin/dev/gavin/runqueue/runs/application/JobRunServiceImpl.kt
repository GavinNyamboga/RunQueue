package dev.gavin.runqueue.runs.application

import dev.gavin.runqueue.common.api.NotFoundException
import dev.gavin.runqueue.common.api.ValidationException
import dev.gavin.runqueue.runs.api.JobRunResponse
import dev.gavin.runqueue.runs.domain.JobRun
import dev.gavin.runqueue.runs.domain.JobRunStatus
import dev.gavin.runqueue.runs.infrastructure.JobRunRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class JobRunServiceImpl(
    private val jobRunRepository: JobRunRepository,
) : JobRunService {

    @Transactional(readOnly = true)
    override fun listAllPaged(pageable: Pageable): Page<JobRunResponse> =
        jobRunRepository.findAll(pageable)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    override fun getById(id: UUID): JobRunResponse =
        jobRunRepository.findById(id)
            .orElseThrow { NotFoundException("JobRun with id $id not found") }
            .toResponse()


    @Transactional(readOnly = true)
    override fun listByJob(jobId: UUID): List<JobRunResponse> =
        jobRunRepository.findAllByJobIdOrderByCreatedAtDesc(jobId)
            .map { it.toResponse() }

    override fun retry(id: UUID): JobRunResponse {
        val run = jobRunRepository.findById(id).orElseThrow { NotFoundException("JobRun with id $id not found") }

        if (run.status != JobRunStatus.FAILED) {
            throw ValidationException("Only FAILED runs can be retried manually")
        }

        val retryRun = JobRun(
            jobId = run.jobId,
            status = JobRunStatus.QUEUED,
            attempt = run.attempt + 1,
            scheduledAt = Instant.now(),
            queuedAt = Instant.now()
        )

        return jobRunRepository.save(retryRun).toResponse()
    }
}
