package inotify

import java.io.IOException
import java.nio.file.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface PathHandler {
    fun handle(path: Path)
}

fun Path.handleExistingFiles(pathHandler: PathHandler) =
        Files.newDirectoryStream(this, object : DirectoryStream.Filter<Path> {
            override fun accept(entry: Path) = !Files.isDirectory(entry)
        }).use { directoryStream ->
            for (path in directoryStream) {
                pathHandler.handle(path)
            }
        }

// Simple class to watch directory events.
class DirectoryWatcher(rootPathArg: String, executor: ExecutorService = Executors.newSingleThreadExecutor()) {

    companion object : WithLogging() {}

    private val rootPath = Paths.get(rootPathArg)
    private val queue = ConcurrentLinkedQueue<Path>()

    init {
        executor.execute({ rootPath.enqueuePaths(this.queue) })
        LOG.warn("STARTED watching dir ${rootPath.toAbsolutePath()}")
    }

    val next: Path?
        get() {
            val result = queue.poll()
            return if (result == null) null else rootPath.resolve(result)
        }

    fun waitNext(sleepInMillis: Long = 5000): Path {
        var result: Path? = null
        while (result == null) {
            result = next
            if (result == null) Thread.sleep(sleepInMillis)
        }
        return result
    }

    fun loop(pathHandler: PathHandler, sleepInMillis: Long = 5000) {
        while (!Thread.currentThread().isInterrupted) {
            pathHandler.handle(waitNext(sleepInMillis))
        }

    }
}

fun String.watchDir(executor: ExecutorService = Executors.newSingleThreadExecutor())
        = DirectoryWatcher(this, executor)

fun String.watchDirLoop(pathHandler: PathHandler, sleepInMillis: Long = 5000, executor: ExecutorService = Executors.newFixedThreadPool(2))
        = executor.execute({ this.watchDir(executor).loop(pathHandler, sleepInMillis) })

fun Path.enqueuePaths(eventQueue: ConcurrentLinkedQueue<Path>,
                      sleepInMillis: Long = 500, kind: WatchEvent.Kind<Path> = StandardWatchEventKinds.ENTRY_CREATE): Unit {

    Thread.currentThread().name = "enqueuePaths[$kind:${this.toAbsolutePath()}]"
    try {

        this.handleExistingFiles(
                object : PathHandler {
                    override fun handle(path: Path) {
                        eventQueue.add(path)
                    }
                })

        val watchService = this@enqueuePaths.fileSystem.newWatchService()
        this@enqueuePaths.register(watchService, kind)

        // loop forever to watch directory
        while (!Thread.currentThread().isInterrupted) {
            val watchKey: WatchKey
            watchKey = watchService.take() // this call is blocking until events are present

            for (event in watchKey.pollEvents()) {
                val path = event.context() as Path
                while (!eventQueue.offer(path)) Thread.sleep(sleepInMillis)
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

object FileEventTest {

    @JvmStatic fun main(vararg args: String) {

        var p: Path? = null
        var i: Int = 0

        val dirWatcher = "/tmp/watch".watchDir()
        while (i++ < 10) {
            p = dirWatcher.waitNext()
            DirectoryWatcher.LOG.warn(i.toString() + ": " + p.toString())
        }

    }
}
