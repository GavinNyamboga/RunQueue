package dev.gavin.runqueue

import dev.gavin.runqueue.jobs.domain.*
import dev.gavin.runqueue.jobs.infrastructure.JobRepository
import dev.gavin.runqueue.runs.domain.JobRun
import dev.gavin.runqueue.runs.domain.JobRunStatus
import dev.gavin.runqueue.runs.infrastructure.ExecutionLogRepository
import dev.gavin.runqueue.runs.infrastructure.JobRunRepository
import dev.gavin.runqueue.scheduler.worker.infrastructure.WorkerHeartbeatRepository
import io.restassured.RestAssured.given
import io.restassured.builder.ResponseBuilder
import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiIntegrationTest : TestContainersConfig() {
    @Autowired
    protected lateinit var jobRepository: JobRepository

    @Autowired
    protected lateinit var jobRunRepository: JobRunRepository

    @Autowired
    protected lateinit var workerHeartbeatRepository: WorkerHeartbeatRepository

    @Autowired
    protected lateinit var executionLogRepository: ExecutionLogRepository

    @LocalServerPort
    private var port: Int = 0

    private val client: HttpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun cleanDatabase() {
        executionLogRepository.deleteAll()
        workerHeartbeatRepository.deleteAll()
        jobRunRepository.deleteAll()
        jobRepository.deleteAll()
    }

    @AfterEach
    fun resetState() {
    }

    protected fun request(): RequestSpecification =
        given()
            .baseUri("http://localhost")
            .port(port)
            .filter(LocalHttpClientFilter(client))

    protected fun saveJob(
        name: String,
        scheduleType: ScheduleType = ScheduleType.ONCE,
        cronExpression: String? = null,
        runAt: Instant? = Instant.parse("2030-01-01T00:00:00Z"),
        status: JobStatus = JobStatus.ACTIVE,
    ): Job = jobRepository.save(
        Job(
            name = name,
            description = "job $name",
            type = JobType.HTTP,
            scheduleType = scheduleType,
            cronExpression = cronExpression,
            runAt = runAt,
            status = status,
            retryStrategy = RetryStrategy.NONE,
            maxRetries = 0,
            retryDelaySeconds = 0,
            timeoutSeconds = 60,
            nextRunAt = if (status == JobStatus.DELETED) null else runAt,
        )
    )

    protected fun saveRun(
        jobId: java.util.UUID,
        status: JobRunStatus,
        attempt: Int = 1,
        scheduledAt: Instant = Instant.parse("2030-01-01T00:00:00Z"),
        queuedAt: Instant? = null,
    ): JobRun = jobRunRepository.save(
        JobRun(
            jobId = jobId,
            status = status,
            attempt = attempt,
            scheduledAt = scheduledAt,
            queuedAt = queuedAt,
        )
    )

    private class LocalHttpClientFilter(
        private val client: HttpClient
    ) : Filter {
        override fun filter(
            requestSpec: FilterableRequestSpecification,
            responseSpec: FilterableResponseSpecification,
            ctx: FilterContext
        ): Response {
            val requestBuilder = HttpRequest.newBuilder(URI.create(requestSpec.uri))
            requestSpec.headers.asList()
                .filterNot { it.name.equals("Content-Length", ignoreCase = true) }
                .forEach { requestBuilder.header(it.name, it.value) }

            val body = requestSpec.getBody<Any>()
            val publisher = when (body) {
                null -> HttpRequest.BodyPublishers.noBody()
                is ByteArray -> HttpRequest.BodyPublishers.ofByteArray(body)
                else -> HttpRequest.BodyPublishers.ofString(body.toString())
            }

            val response = client.send(
                requestBuilder.method(requestSpec.method, publisher).build(),
                HttpResponse.BodyHandlers.ofByteArray()
            )

            val builder = ResponseBuilder()
                .setStatusCode(response.statusCode())
                .setBody(response.body())

            response.headers().map().forEach { (name, values) ->
                values.forEach { builder.setHeader(name, it) }
            }

            return builder.build()
        }
    }
}
