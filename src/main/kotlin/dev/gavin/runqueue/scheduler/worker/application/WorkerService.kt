package dev.gavin.runqueue.scheduler.worker.application

interface WorkerService {
    fun processQueuedRUns()
    fun recordHeartbeat()
}
