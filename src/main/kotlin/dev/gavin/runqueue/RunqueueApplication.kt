package dev.gavin.runqueue

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class RunqueueApplication

fun main(args: Array<String>) {
    runApplication<RunqueueApplication>(*args)
}
