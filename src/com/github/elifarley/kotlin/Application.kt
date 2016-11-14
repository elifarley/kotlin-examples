package com.orgecc.myproj

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.nio.file.Path
import java.util.concurrent.Executors

@SpringBootApplication
@EntityScan(basePackages = { "com.orgecc.persistence", "com.orgecc.myproj.model" })
@EnableJpaRepositories(basePackages = { "com.orgecc.myproj.repository" })
open class Application {

    private val executorService = Executors.newFixedThreadPool(2)

    private val pathHandler = object: PathHandler {
        override fun handle(path: Path) {
            println("Path: $path")
        }
    }

    @Bean
    open fun myDirWatcherBean(): Any {
        return dirWatcherBean(
                "/tmp/watch", pathHandler, 2 * 1000, executorService)
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

