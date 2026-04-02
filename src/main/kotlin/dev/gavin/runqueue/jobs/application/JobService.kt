package dev.gavin.runqueue.jobs.application

import dev.gavin.runqueue.jobs.api.CreateJobRequest
import dev.gavin.runqueue.jobs.api.JobResponse
import dev.gavin.runqueue.jobs.api.UpdateJobRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface JobService {
    fun createJob(request: CreateJobRequest): JobResponse
    fun getById(id: UUID): JobResponse
    fun listJobsPaged(pageable: Pageable): Page<JobResponse>
    fun updateJob(id: UUID, request: UpdateJobRequest): JobResponse
    fun pauseJob(id: UUID): JobResponse
    fun resumeJob(id: UUID): JobResponse
    fun deleteJob(id: UUID)
}