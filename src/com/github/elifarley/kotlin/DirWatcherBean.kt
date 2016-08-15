package inotify

import inotify.PathHandler
import inotify.watchDir
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

fun dirWatcherBean(dir: String, pathHandler: PathHandler,
                   sleepInMillis: Long = 5000, executorService: ExecutorService) = object {
    @PostConstruct
    fun init() {
        val dirWatcher = "/tmp/watch".watchDir(executorService)
        executorService.execute({ dirWatcher.loop(pathHandler, sleepInMillis) })
        executorService.shutdown()
    }

    @PreDestroy
    fun beanDestroy() {
        if (executorService == null) return
        try {
            // wait 1 second for closing all threads
            executorService.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

    }

}
