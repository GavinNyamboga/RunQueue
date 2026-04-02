package dev.gavin.runqueue.runs.application

import dev.gavin.runqueue.runs.api.JobRunResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface JobRunService {
    fun listAllPaged(pageable: Pageable): Page<JobRunResponse>
    fun getById(id: UUID): JobRunResponse
    fun listByJob(jobId: UUID): List<JobRunResponse>
    fun retry(id: UUID): JobRunResponse
}
