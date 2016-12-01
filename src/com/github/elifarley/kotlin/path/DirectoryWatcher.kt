package com.orgecc.util.path

import com.m4u.util.WithLogging
import com.m4u.util.logger
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface PathHandler {
    fun handle(path: Path)
}

fun Path.handleAndDelete(pathHandler: (Path) -> Unit): Unit =
        try {
            DirectoryWatcher.LOG.warn("Will handle '$this'")
            pathHandler(this)
            DirectoryWatcher.LOG.debug("Done. Deleting '$this'...")

            try {
                Files.delete(this)

            } catch(e: Exception) {
                DirectoryWatcher.LOG.error("Unable to delete '$this'")
            }

        } catch (e: Exception) {
            logger(pathHandler.javaClass).error("Unable to handle file '$this'", e)

            this.parent.resolveSibling("error").let { errorPath ->
                Files.createDirectories(errorPath)
                var errorFile = errorPath.resolve(this.fileName)
                var tries = 0
                while (tries  < 1000 && Files.exists(errorFile)) {
                    tries++
                    errorFile = File("$errorFile.$tries").toPath()
                }
                Files.move(this, errorFile)
            }
        }

fun Path.enqueueExistingFiles(target: MutableCollection<Path>) =
        Files.newDirectoryStream(this) {
            !Files.isDirectory(it) &&
                    !it.fileName.toString().startsWith(".")
        }.use { directoryStream ->
            target += directoryStream
        }

// Simple class to watch directory events.
class DirectoryWatcher(rootPathArg: String, executor: ExecutorService = Executors.newSingleThreadExecutor()) {

    companion object : WithLogging()

    private val rootPath = Paths.get(rootPathArg)
    private val queue = ConcurrentLinkedQueue<Path>()

    init {
        executor.execute { rootPath.enqueuePaths(this.queue) }
        LOG.warn("STARTED watching dir ${rootPath.toAbsolutePath()}")
    }

    val next: Path?
        get() = queue.poll()?.let {
            rootPath.resolve(it)
        }

    fun waitNext(sleepInMillis: Long = 5000): Path {
        var result: Path? = null
        while (result == null) {
            result = next
            if (result == null) Thread.sleep(sleepInMillis)
        }
        return result
    }

    fun loop(pathHandler: (Path) -> Unit, sleepInMillis: Long = 5000) {
        while (!Thread.currentThread().isInterrupted) {
            waitNext(sleepInMillis).handleAndDelete(pathHandler)

        }

    }

}

fun String.watchDir(executor: ExecutorService = Executors.newSingleThreadExecutor())
        = DirectoryWatcher(this, executor)

fun String.watchDirLoop(pathHandler: (Path) -> Unit, sleepInMillis: Long = 5000, executor: ExecutorService = Executors.newFixedThreadPool(2))
        = executor.execute { this.watchDir(executor).loop(pathHandler, sleepInMillis) }

fun Path.enqueuePaths(eventQueue: ConcurrentLinkedQueue<Path>,
                      sleepInMillis: Long = 500, kind: WatchEvent.Kind<Path> = StandardWatchEventKinds.ENTRY_CREATE): Unit {

    Thread.currentThread().name = "enqueuePaths[$kind:${this.toAbsolutePath()}]"

    Files.createDirectories(this)

    try {

        this.enqueueExistingFiles(eventQueue)

        val watchService = this@enqueuePaths.fileSystem.newWatchService()
        this@enqueuePaths.register(watchService, kind)

        // loop forever to watch directory
        while (!Thread.currentThread().isInterrupted) {

            val watchKey: WatchKey
            watchKey = watchService.take() // this call is blocking until events are present

            watchKey.pollEvents().map { it.context() as Path }.forEach {
                while (!eventQueue.offer(it)) Thread.sleep(sleepInMillis)
            }

            // if the watched directed gets deleted, break loop
            if (!watchKey.reset()) {
                DirectoryWatcher.LOG.warn("[enqueuePaths] No longer valid")
                watchKey.cancel()
                watchService.close()
                break
            }
        }

    } catch (ex: InterruptedException) {
        DirectoryWatcher.LOG.warn("Interrupted. Goodbye")

    } catch (ex: IOException) {
        DirectoryWatcher.LOG.warn("", ex)
    }

}

fun Path.enqueueEvents(eventQueue: ConcurrentLinkedQueue<WatchEvent<*>>,
                       sleepInMillis: Long = 500, vararg kinds: WatchEvent.Kind<*> = arrayOf(StandardWatchEventKinds.ENTRY_CREATE)): Unit {

    Thread.currentThread().name = "enqueueEvents[$kinds:${this.toAbsolutePath()}]"

    try {

        val watchService = this@enqueueEvents.fileSystem.newWatchService()
        this@enqueueEvents.register(watchService, *kinds)

        // loop forever to watch directory
        while (!Thread.currentThread().isInterrupted) {
            val watchKey: WatchKey
            watchKey = watchService.take() // this call is blocking until events are present

            for (event in watchKey.pollEvents()) {
                while (!eventQueue.offer(event)) Thread.sleep(sleepInMillis)
            }


            // if the watched directed gets deleted, break loop
            if (!watchKey.reset()) {
                DirectoryWatcher.LOG.warn("No longer valid")
                watchKey.cancel()
                watchService.close()
                break
            }
        }

    } catch (ex: InterruptedException) {
        DirectoryWatcher.LOG.warn("Interrupted. Goodbye")

    } catch (ex: IOException) {
        DirectoryWatcher.LOG.warn("", ex)
    }

}
