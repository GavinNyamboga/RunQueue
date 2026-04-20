package dev.gavin.runqueue.jobs.application

import dev.gavin.runqueue.jobs.api.RecurringIntervalUnit
import dev.gavin.runqueue.jobs.api.RecurringScheduleMode
import dev.gavin.runqueue.jobs.api.RecurringScheduleRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RecurringScheduleMapperTest {
    @Test
    fun `weekly schedule maps to cron and back`() {
        val schedule = RecurringScheduleRequest(
            mode = RecurringScheduleMode.WEEKLY,
            dayOfWeek = "MONDAY",
            timeOfDay = "09:30"
        )

        val cronExpression = RecurringScheduleMapper.toCronExpression(schedule)
        val mappedBack = RecurringScheduleMapper.fromCronExpression(cronExpression)

        assertEquals("0 30 9 * * 1", cronExpression)
        assertEquals(RecurringScheduleMode.WEEKLY, mappedBack?.mode)
        assertEquals("MONDAY", mappedBack?.dayOfWeek)
        assertEquals("09:30", mappedBack?.timeOfDay)
    }

    @Test
    fun `every hours schedule maps to cron and back`() {
        val schedule = RecurringScheduleRequest(
            mode = RecurringScheduleMode.EVERY,
            interval = 2,
            intervalUnit = RecurringIntervalUnit.HOURS
        )

        val cronExpression = RecurringScheduleMapper.toCronExpression(schedule)
        val mappedBack = RecurringScheduleMapper.fromCronExpression(cronExpression)

        assertEquals("0 0 */2 * * *", cronExpression)
        assertEquals(RecurringScheduleMode.EVERY, mappedBack?.mode)
        assertEquals(2, mappedBack?.interval)
        assertEquals(RecurringIntervalUnit.HOURS, mappedBack?.intervalUnit)
    }
}
