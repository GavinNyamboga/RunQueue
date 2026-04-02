package dev.gavin.runqueue.jobs.api

import dev.gavin.runqueue.jobs.application.JobService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/jobs")
class JobController(
    private val jobService: JobService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateJobRequest): JobResponse = jobService.createJob(request)

    @GetMapping
    fun list(
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<JobResponse> = jobService.listJobsPaged(pageable)

    @GetMapping("/{jobId}")
    fun getById(@PathVariable jobId: UUID): JobResponse = jobService.getById(jobId)

    @PutMapping("/{jobId}")
    fun update(@PathVariable jobId: UUID, @Valid @RequestBody request: UpdateJobRequest): JobResponse =
        jobService.updateJob(jobId, request)

    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable jobId: UUID) {
        jobService.deleteJob(jobId)
    }
}
