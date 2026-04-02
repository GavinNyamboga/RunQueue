package dev.gavin.runqueue.jobs.application

import dev.gavin.runqueue.jobs.domain.ScheduleType
import java.time.Instant

interface ScheduleCalculator {
    fun calculateFirstRun(scheduleType: ScheduleType, cronExpression: String?, runAt: Instant?): Instant?
    fun calculateNextRun(scheduleType: ScheduleType, cronExpression: String?, lastRunAt: Instant): Instant?
}