package dev.gavin.runqueue.jobs.application

import dev.gavin.runqueue.jobs.domain.ScheduleType
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

@Component
class DefaultScheduleCalculator : ScheduleCalculator {
    override fun calculateFirstRun(
        scheduleType: ScheduleType,
        cronExpression: String?,
        runAt: Instant?
    ): Instant? {
        return when (scheduleType) {
            ScheduleType.ONCE -> runAt
            ScheduleType.CRON -> {
                require(!cronExpression.isNullOrBlank()) { "cronExpression is required for CRON jobs" }

                val expr = CronExpression.parse(cronExpression)
                expr.next(java.time.LocalDateTime.now(ZoneOffset.UTC))?.toInstant(ZoneOffset.UTC)
            }
        }
    }

    override fun calculateNextRun(
        scheduleType: ScheduleType,
        cronExpression: String?,
        lastRunAt: Instant
    ): Instant? {
        return when (scheduleType) {
            ScheduleType.ONCE -> null
            ScheduleType.CRON -> {
                require(!cronExpression.isNullOrBlank()) { "cronExpression is required for CRON jobs" }

                val expr = CronExpression.parse(cronExpression)
                expr.next(lastRunAt.atZone(ZoneOffset.UTC).toLocalDateTime())?.toInstant(ZoneOffset.UTC)
            }
        }
    }

}