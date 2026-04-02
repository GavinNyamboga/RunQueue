package dev.gavin.runqueue.scheduler.scheduler.application

import java.util.*

interface SchedulerService {
    fun scheduleDueJobs()
    fun scheduleJobIfDue(jobId: UUID): Boolean
}
