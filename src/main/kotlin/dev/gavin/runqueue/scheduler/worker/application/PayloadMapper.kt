package dev.gavin.runqueue.scheduler.worker.application

import com.fasterxml.jackson.databind.ObjectMapper
import dev.gavin.runqueue.jobs.domain.Job
import dev.gavin.runqueue.jobs.domain.JobType
import org.springframework.stereotype.Component

@Component
class PayloadMapper(
    private val objectMapper: ObjectMapper,
) {
    fun toPayload(job: Job): JobPayload {
        val json = job.payloadJson ?: "{}"

        return when (job.type) {
            JobType.HTTP -> objectMapper.readValue(json, HttpJobPayload::class.java)
            JobType.MOCK -> objectMapper.readValue(json, MockJobPayload::class.java)
        }
    }
}