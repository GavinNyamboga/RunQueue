# RunQueue

RunQueue is a Kotlin and Spring Boot service for scheduling and executing background jobs.

It stores job definitions in PostgreSQL, schedules due work automatically, records every execution attempt, retries failed runs based on the job policy, and tracks worker liveness with heartbeats.

## What It Does

- creates and manages scheduled jobs through a REST API
- supports one-time and cron-based schedules
- executes `HTTP` and `MOCK` jobs
- records run history in `job_runs`
- retries failed runs with `NONE`, `FIXED_DELAY`, or `EXPONENTIAL_BACKOFF`
- supports manual retry of failed runs
- tracks workers through `worker_heartbeats`

## API

Jobs:

- `POST /api/jobs`
- `GET /api/jobs`
- `GET /api/jobs/{jobId}`
- `PUT /api/jobs/{jobId}`
- `DELETE /api/jobs/{jobId}`

Runs:

- `GET /api/runs`
- `GET /api/runs/{id}`
- `POST /api/runs/{id}/retry`

## Architecture

RunQueue is organized into three main parts:

1. Jobs API
   Persists job definitions, validates schedules, and calculates `nextRunAt`.
2. Scheduler
   Scans for due jobs and creates `QUEUED` entries in `job_runs`.
3. Worker
   Polls queued runs, executes them, updates status, handles retries, records heartbeats, and recovers runs from stale workers.

Core tables:

- `jobs`: job definition, schedule, payload, retry policy
- `job_runs`: execution attempts and retry state
- `execution_logs`: run-level execution log storage
- `worker_heartbeats`: worker status and last heartbeat time

## Supported Model

- `ScheduleType.ONCE`
- `ScheduleType.CRON`
- `JobType.HTTP`
- `JobType.MOCK`
- `RetryStrategy.NONE`
- `RetryStrategy.FIXED_DELAY`
- `RetryStrategy.EXPONENTIAL_BACKOFF`

Job request rules:

- `name` is required and unique
- `scheduleType=ONCE` requires `runAt`
- `scheduleType=CRON` requires `cronExpression`
- `maxRetries` must be between `0` and `20`

## Stack

- Kotlin 2.2
- Spring Boot 4
- Spring MVC
- Spring Data JPA
- Flyway
- PostgreSQL
- Testcontainers
- Rest Assured

## Requirements

- Java 24
- Docker

## Configuration

Runtime configuration is environment-driven.

The project includes:

- `.env.example`: example environment values
- `.env`: local Docker Compose defaults
- `application.yml`: Spring configuration with environment placeholders

Main variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

## Local Setup

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
./gradlew bootRun
```

Flyway migrations run automatically on startup.

## Run With Docker

Build and start the full stack:

```bash
docker compose up --build
```

This starts:

- `postgres`
- `app`

The API will be available at:

```text
http://localhost:8080
```

To stop the stack:

```bash
docker compose down
```

To stop the stack and remove the database volume:

```bash
docker compose down -v
```

## Docker Development Notes

The Docker setup runs the packaged application jar built from the `Dockerfile`. That means source code changes are not reflected automatically inside the running container.

After changing Kotlin code, resources, Gradle config, or the Dockerfile, rebuild the app image:

```bash
docker compose up --build
```

If the stack is already running, you can rebuild just the app service:

```bash
docker compose build app
docker compose up -d app
```

For a faster development loop, run only Postgres in Docker and run the app locally:

```bash
docker compose up -d postgres
./gradlew bootRun
```

## Example Requests

Create a one-time HTTP job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "nightly-report",
    "description": "Calls a downstream webhook once",
    "type": "HTTP",
    "scheduleType": "ONCE",
    "runAt": "2030-03-10T15:30:00Z",
    "payload": {
      "method": "POST",
      "url": "https://example.com/hook",
      "headers": {
        "Content-Type": "application/json"
      },
      "body": "{\"report\":\"daily\"}"
    },
    "retryStrategy": "FIXED_DELAY",
    "maxRetries": 3,
    "retryDelaySeconds": 120,
    "timeoutSeconds": 90
  }'
```

Create a cron mock job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "synthetic-health-check",
    "type": "MOCK",
    "scheduleType": "CRON",
    "cronExpression": "0 */5 * * * *",
    "payload": {
      "durationMillis": 500,
      "shouldFail": false
    },
    "retryStrategy": "EXPONENTIAL_BACKOFF",
    "maxRetries": 4,
    "retryDelaySeconds": 30,
    "timeoutSeconds": 10
  }'
```

List jobs:

```bash
curl http://localhost:8080/api/jobs
```

List runs:

```bash
curl http://localhost:8080/api/runs
```

Retry a failed run:

```bash
curl -X POST http://localhost:8080/api/runs/{runId}/retry
```

## Tests

Run the full test suite:

```bash
./gradlew test
```

Tests use Testcontainers to start a disposable PostgreSQL instance automatically.

Current automated coverage includes:

- jobs API integration tests
- runs API integration tests
- scheduler and worker integration tests
- heartbeat and stale-worker recovery tests

## Project Layout

```text
src/main/kotlin/dev/gavin/runqueue
├── common/      # shared error handling and base entities
├── jobs/        # job API, service, domain, repository
├── runs/        # run API, service, domain, repository
└── scheduler/   # scheduler and worker execution
```
