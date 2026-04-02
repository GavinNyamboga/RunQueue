package dev.gavin.runqueue.scheduler.worker.application

import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.domain.JobType
import dev.gavin.runqueue.runs.domain.JobRun

interface JobExecutor {
    fun supports(jobType: JobType): Boolean
    fun execute(job: Job, run: JobRun): ExecutionResult
}