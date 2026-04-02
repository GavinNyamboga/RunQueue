package dev.gavin.runqueue.scheduler.worker.application

import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.domain.JobType
import dev.gavin.runqueue.runs.domain.JobRun
import org.springframework.stereotype.Component

@Component
class MockJobExecutor(
    private val payloadMapper: PayloadMapper
) : JobExecutor {

    override fun supports(jobType: JobType): Boolean = jobType == JobType.MOCK

    override fun execute(job: Job, run: JobRun): ExecutionResult {
        val payload = payloadMapper.toPayload(job) as MockJobPayload

        Thread.sleep(payload.durationMillis)

        return if (payload.shouldFail) {
            ExecutionFailure(message = payload.failureMessage ?: "Mock job failed")
        } else {
            ExecutionSuccess(
                output = mapOf(
                    "message" to "Mock job completed",
                    "runId" to run.id.toString()
                )
            )
        }
    }
}