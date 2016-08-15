package inotify

import com.m4u.cielo.ws.pushnotification.unit
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@SpringBootApplication
open class Application {

    private val executorService = Executors.newFixedThreadPool(2)

    @Bean
    open fun myDirWatcherBean() = object {
        @PostConstruct
        fun init() {
            val dirWatcher = "/tmp/watch".watchDir(executorService)
            executorService.execute({ handleNewFile(dirWatcher)})
            executorService.shutdown()
        }

        private fun handleNewFile(dirWatcher: DirectoryWatcher) {
            while (!Thread.currentThread().isInterrupted) {
                var p = dirWatcher.waitNext(5 * 1000 )
                println(p.toString())
            }
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

}

inline fun unit(f: () -> Any): Unit {
    f()
}

inline fun unit(vararg funs: () -> Any): Unit {
    for (f in funs) f()
}

fun main(vararg args: String) =
        unit { SpringApplication.run(Application::class.java, *args) }

