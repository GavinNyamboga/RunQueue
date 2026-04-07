package dev.gavin.runqueue

import dev.gavin.runqueue.runs.domain.JobRunStatus
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.*

class RunsApiIntegrationTest : ApiIntegrationTest() {
    @Test
    fun `list runs returns latest first`() {
        val job = saveJob(name = "runs-list-job")
        val older = saveRun(job.id, status = JobRunStatus.SUCCESS, scheduledAt = Instant.parse("2030-01-01T00:00:00Z"))
        val newer = saveRun(job.id, status = JobRunStatus.FAILED, scheduledAt = Instant.parse("2030-01-02T00:00:00Z"))

        request()
            .get("/api/runs")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content", hasSize<Any>(2))
            .body("content[0].id", equalTo(newer.id.toString()))
            .body("content[1].id", equalTo(older.id.toString()))
    }

    @Test
    fun `get run by id returns persisted run`() {
        val job = saveJob(name = "run-lookup-job")
        val run = saveRun(job.id, status = JobRunStatus.RUNNING, queuedAt = Instant.parse("2030-01-01T00:05:00Z"))

        request()
            .get("/api/runs/{id}", run.id)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(run.id.toString()))
            .body("jobId", equalTo(job.id.toString()))
            .body("status", equalTo("RUNNING"))
            .body("queuedAt", equalTo("2030-01-01T00:05:00Z"))
    }

    @Test
    fun `get run by id returns 404 when missing`() {
        request()
            .get("/api/runs/{id}", UUID.randomUUID())
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("error", equalTo("Not Found"))
    }

    @Test
    fun `retry failed run creates queued retry run`() {
        val job = saveJob(name = "retry-job")
        val failedRun = saveRun(job.id, status = JobRunStatus.FAILED, attempt = 2)

        val response =
            request()
                .post("/api/runs/{id}/retry", failedRun.id)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("jobId", equalTo(job.id.toString()))
                .body("status", equalTo("QUEUED"))
                .body("attempt", equalTo(3))
                .extract()

        val persisted = jobRunRepository.findById(UUID.fromString(response.path("id"))).orElseThrow()
        kotlin.test.assertEquals(JobRunStatus.QUEUED, persisted.status)
        kotlin.test.assertEquals(3, persisted.attempt)
        kotlin.test.assertEquals(job.id, persisted.jobId)
    }

    @Test
    fun `retry returns 400 for non failed run`() {
        val job = saveJob(name = "no-retry-job")
        val run = saveRun(job.id, status = JobRunStatus.SUCCESS)

        request()
            .post("/api/runs/{id}/retry", run.id)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("Only FAILED runs can be retried manually"))
    }

    @Test
    fun `retry returns 404 when run is missing`() {
        request()
            .post("/api/runs/{id}/retry", UUID.randomUUID())
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("error", equalTo("Not Found"))
    }
}
