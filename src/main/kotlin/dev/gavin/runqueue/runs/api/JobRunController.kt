package dev.gavin.runqueue.runs.api

import dev.gavin.runqueue.runs.application.JobRunService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/runs")
class JobRunController(
    private val jobRunService: JobRunService
) {

    @GetMapping
    fun listAll(
        @PageableDefault(sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): Page<JobRunResponse> =
        jobRunService.listAllPaged(pageable)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): JobRunResponse =
        jobRunService.getById(id)

    @PostMapping("/{id}/retry")
    fun retry(@PathVariable id: UUID): JobRunResponse =
        jobRunService.retry(id)
}
