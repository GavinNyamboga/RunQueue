package dev.gavin.runqueue.scheduler.worker.application

import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.domain.JobType
import dev.gavin.runqueue.runs.domain.JobRun
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Component
class HttpJobExecutor(
    private val payloadMapper: PayloadMapper
) : JobExecutor {

    private val restTemplate = RestTemplate()

    override fun supports(jobType: JobType): Boolean = jobType == JobType.HTTP

    override fun execute(job: Job, run: JobRun): ExecutionResult {
        val payload = payloadMapper.toPayload(job) as HttpJobPayload

        return try {
            val headers = HttpHeaders()
            payload.headers.forEach { (k, v) -> headers[k] = v }

            val request = HttpEntity(payload.body, headers)
            val response = restTemplate.exchange<String>(
                payload.url,
                HttpMethod.valueOf(payload.method.uppercase()),
                request
            )

            ExecutionSuccess(
                output = mapOf(
                    "statusCode" to response.statusCode.value(),
                    "responseBody" to response.body
                )
            )
        } catch (ex: Exception) {
            ExecutionFailure(message = ex.message ?: "HTTP execution failed")
        }
    }
}