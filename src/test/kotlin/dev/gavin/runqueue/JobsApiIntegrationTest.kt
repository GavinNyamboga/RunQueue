package dev.gavin.runqueue

import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.*

class JobsApiIntegrationTest : ApiIntegrationTest() {
    @Test
    fun `create job persists and returns job payload`() {
        val runAt = Instant.parse("2030-03-10T15:30:00Z")

        val response =
            request()
                .contentType("application/json")
                .body(
                    """
                    {
                      "name": "nightly-report",
                      "description": "Runs once",
                      "type": "HTTP",
                      "scheduleType": "ONCE",
                      "runAt": "$runAt",
                      "payload": {
                        "url": "https://example.com/hook"
                      },
                      "retryStrategy": "FIXED_DELAY",
                      "maxRetries": 3,
                      "retryDelaySeconds": 120,
                      "timeoutSeconds": 90
                    }
                    """.trimIndent()
                )
                .post("/api/jobs")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("name", equalTo("nightly-report"))
                .body("description", equalTo("Runs once"))
                .body("status", equalTo("ACTIVE"))
                .body("scheduleType", equalTo("ONCE"))
                .body("runAt", equalTo(runAt.toString()))
                .body("nextRunAt", equalTo(runAt.toString()))
                .extract()

        val persisted = jobRepository.findById(UUID.fromString(response.path("id"))).orElseThrow()
        kotlin.test.assertEquals("nightly-report", persisted.name)
        kotlin.test.assertEquals(runAt, persisted.nextRunAt)
        kotlin.test.assertEquals("""{"url":"https://example.com/hook"}""", persisted.payloadJson)
    }

    @Test
    fun `create cron job accepts recurring schedule instead of raw cron`() {
        val response =
            request()
                .contentType("application/json")
                .body(
                    """
                    {
                      "name": "every-two-hours",
                      "type": "MOCK",
                      "scheduleType": "CRON",
                      "recurringSchedule": {
                        "mode": "EVERY",
                        "interval": 2,
                        "intervalUnit": "HOURS"
                      }
                    }
                    """.trimIndent()
                )
                .post("/api/jobs")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("scheduleType", equalTo("CRON"))
                .body("cronExpression", equalTo("0 0 */2 * * *"))
                .body("recurringSchedule.mode", equalTo("EVERY"))
                .body("recurringSchedule.interval", equalTo(2))
                .body("recurringSchedule.intervalUnit", equalTo("HOURS"))
                .extract()

        val persisted = jobRepository.findById(UUID.fromString(response.path("id"))).orElseThrow()
        kotlin.test.assertEquals("0 0 */2 * * *", persisted.cronExpression)
    }

    @Test
    fun `list jobs returns latest first`() {
        val older = saveJob(name = "older-job")
        val newer = saveJob(name = "newer-job")

        request()
            .get("/api/jobs")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("content", hasSize<Any>(2))
            .body("content[0].id", equalTo(newer.id.toString()))
            .body("content[1].id", equalTo(older.id.toString()))
    }

    @Test
    fun `get job by id returns persisted job`() {
        val job = saveJob(name = "lookup-job")

        request()
            .get("/api/jobs/{jobId}", job.id)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(job.id.toString()))
            .body("name", equalTo("lookup-job"))
            .body("status", equalTo("ACTIVE"))
    }

    @Test
    fun `get job by id returns 404 when missing`() {
        request()
            .get("/api/jobs/{jobId}", UUID.randomUUID())
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("error", equalTo("Not Found"))
    }

    @Test
    fun `update job mutates editable fields`() {
        val job = saveJob(name = "mutable-job")
        val updatedRunAt = Instant.parse("2031-04-12T10:15:30Z")

        request()
            .contentType("application/json")
            .body(
                """
                {
                  "description": "Updated description",
                  "runAt": "$updatedRunAt",
                  "payload": {
                    "priority": "high"
                  },
                  "maxRetries": 5,
                  "retryDelaySeconds": 30,
                  "timeoutSeconds": 120
                }
                """.trimIndent()
            )
            .put("/api/jobs/{jobId}", job.id)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("description", equalTo("Updated description"))
            .body("runAt", equalTo(updatedRunAt.toString()))
            .body("maxRetries", equalTo(5))
            .body("retryDelaySeconds", equalTo(30))
            .body("timeoutSeconds", equalTo(120))

        val persisted = jobRepository.findById(job.id).orElseThrow()
        kotlin.test.assertEquals(updatedRunAt, persisted.runAt)
        kotlin.test.assertEquals("""{"priority":"high"}""", persisted.payloadJson)
    }

    @Test
    fun `update cron job can replace raw cron with recurring schedule`() {
        val job = saveJob(
            name = "recurring-update",
            scheduleType = dev.gavin.runqueue.jobs.domain.ScheduleType.CRON,
            cronExpression = "0 */5 * * * *",
            runAt = null
        )

        request()
            .contentType("application/json")
            .body(
                """
                {
                  "recurringSchedule": {
                    "mode": "WEEKLY",
                    "dayOfWeek": "MONDAY",
                    "timeOfDay": "09:30"
                  }
                }
                """.trimIndent()
            )
            .put("/api/jobs/{jobId}", job.id)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("cronExpression", equalTo("0 30 9 * * 1"))
            .body("recurringSchedule.mode", equalTo("WEEKLY"))
            .body("recurringSchedule.dayOfWeek", equalTo("MONDAY"))
            .body("recurringSchedule.timeOfDay", equalTo("09:30"))

        val persisted = jobRepository.findById(job.id).orElseThrow()
        kotlin.test.assertEquals("0 30 9 * * 1", persisted.cronExpression)
    }

    @Test
    fun `delete job marks it as deleted`() {
        val job = saveJob(name = "delete-me")

        request()
            .delete("/api/jobs/{jobId}", job.id)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        val persisted = jobRepository.findById(job.id).orElseThrow()
        kotlin.test.assertEquals("DELETED", persisted.status.name)
        kotlin.test.assertEquals(null, persisted.nextRunAt)
    }

    @Test
    fun `create job returns 400 when once schedule is missing runAt`() {
        request()
            .contentType("application/json")
            .body(
                """
                {
                  "name": "missing-run-at",
                  "type": "HTTP",
                  "scheduleType": "ONCE"
                }
                """.trimIndent()
            )
            .post("/api/jobs")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("runAt is required for ONCE jobs"))
    }

    @Test
    fun `create job returns 400 when cron schedule includes runAt`() {
        request()
            .contentType("application/json")
            .body(
                """
                {
                  "name": "invalid-cron",
                  "type": "HTTP",
                  "scheduleType": "CRON",
                  "cronExpression": "0 0 * * * *",
                  "runAt": "2030-01-01T00:00:00Z"
                }
                """.trimIndent()
            )
            .post("/api/jobs")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("runAt is not supported for CRON jobs"))
    }

    @Test
    fun `create job returns 400 when cron schedule is missing cron and recurring schedule`() {
        request()
            .contentType("application/json")
            .body(
                """
                {
                  "name": "missing-recurring",
                  "type": "HTTP",
                  "scheduleType": "CRON"
                }
                """.trimIndent()
            )
            .post("/api/jobs")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("cronExpression or recurringSchedule is required for CRON jobs"))
    }

    @Test
    fun `create job returns 400 when cron expression and recurring schedule are both provided`() {
        request()
            .contentType("application/json")
            .body(
                """
                {
                  "name": "duplicate-schedule-input",
                  "type": "HTTP",
                  "scheduleType": "CRON",
                  "cronExpression": "0 */5 * * * *",
                  "recurringSchedule": {
                    "mode": "DAILY",
                    "timeOfDay": "09:30"
                  }
                }
                """.trimIndent()
            )
            .post("/api/jobs")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("Provide either cronExpression or recurringSchedule, not both"))
    }

    @Test
    fun `schedule options lists available recurring modes and units`() {
        request()
            .get("/api/jobs/schedule-options")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("recurringModes", hasItems("EVERY", "DAILY", "WEEKLY", "MONTHLY"))
            .body("intervalUnits", hasItems("MINUTES", "HOURS"))
    }

    @Test
    fun `create job returns 400 for bean validation errors`() {
        request()
            .contentType("application/json")
            .body(
                """
                {
                  "name": "",
                  "type": "HTTP",
                  "scheduleType": "ONCE",
                  "runAt": "2030-01-01T00:00:00Z"
                }
                """.trimIndent()
            )
            .post("/api/jobs")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("name: must not be blank"))
    }

    @Test
    fun `create job returns 400 when max retries exceeds cap`() {
        request()
            .contentType("application/json")
            .body(
                """
                {
                  "name": "too-many-retries",
                  "type": "HTTP",
                  "scheduleType": "ONCE",
                  "runAt": "2030-01-01T00:00:00Z",
                  "maxRetries": 21
                }
                """.trimIndent()
            )
            .post("/api/jobs")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo("maxRetries: must be less than or equal to 20"))
    }
}
