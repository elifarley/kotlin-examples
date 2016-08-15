package inotify

import java.io.IOException
import java.nio.file.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface PathHandler {
    fun handle(path: Path)
}

// Simple class to watch directory events.
class DirectoryWatcher(rootPathArg: String, executor: ExecutorService = Executors.newSingleThreadExecutor()) {

    private val rootPath = Paths.get(rootPathArg)
    private val queue = rootPath.enqueueEvents(executor = executor)

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

fun Path.enqueueEvents(eventQueue: ConcurrentLinkedQueue<Path> = ConcurrentLinkedQueue<Path>(),
                       sleepInMillis: Long = 500, executor: ExecutorService = Executors.newSingleThreadExecutor(),
                       kind: WatchEvent.Kind<Path> = StandardWatchEventKinds.ENTRY_CREATE): ConcurrentLinkedQueue<Path> {
    executor.execute({
        Thread.currentThread().name = "enqueueEvents[$kind:${this.toAbsolutePath()}]"
        try {
            val watchService = this@enqueueEvents.fileSystem.newWatchService()
            this@enqueueEvents.register(watchService, kind)

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
                    println("No longer valid")
                    watchKey.cancel()
                    watchService.close()
                    break
                }
            }

        } catch (ex: InterruptedException) {
            println("Interrupted. Goodbye")

        } catch (ex: IOException) {
            ex.printStackTrace()  // don't do this in production code.
        }

    })
    return eventQueue

}

fun Path.enqueueEvents(eventQueue: ConcurrentLinkedQueue<WatchEvent<*>>,
                       sleepInMillis: Long = 500, executor: ExecutorService = Executors.newSingleThreadExecutor(),
                       vararg kinds: WatchEvent.Kind<*> = arrayOf(StandardWatchEventKinds.ENTRY_CREATE)): Path {
    executor.execute({
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
                    println("No longer valid")
                    watchKey.cancel()
                    watchService.close()
                    break
                }
            }

        } catch (ex: InterruptedException) {
            println("Interrupted. Goodbye")

        } catch (ex: IOException) {
            ex.printStackTrace()  // don't do this in production code.
        }
    })
    return this@enqueueEvents

}

object FileEventTest {

    @JvmStatic fun main(vararg args: String) {

        var p: Path? = null
        var i: Int = 0

        val dirWatcher = "/tmp/watch".watchDir()
        while (i++ < 10) {
            p = dirWatcher.waitNext()
            println(i.toString() + ": " + p.toString())
        }

    }
}
