package com.orgecc.util.file

import com.orgecc.util.WithLogging
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@JvmOverloads
fun dirWatcherBean(dir: String, pathHandler: PathHandler,
                   sleepInMillis: Long = 5000, executorService: ExecutorService = Executors.newFixedThreadPool(2)
) = object : WithLogging() {
    // Packages and file facades are not yet supported in Kotlin reflection. Meanwhile please use Java reflection to inspect DirWatcherBeanKt

    @PostConstruct
    fun init() {
        dir.watchDirLoop( { pathHandler.handle(it) }, sleepInMillis, executorService)
    }

    @PreDestroy
    fun beanDestroy() =
        try {
            LOG.warn("[dirWatcherBean PreDestroy START]")
            // wait 1 second for closing all threads
            executorService.awaitTermination(1, TimeUnit.SECONDS)
            LOG.warn("[dirWatcherBean PreDestroy END]")

        } catch (e: InterruptedException) {
            LOG.warn("[dirWatcherBean INTERRUPTED]")
            Thread.currentThread().interrupt()
        }

}
