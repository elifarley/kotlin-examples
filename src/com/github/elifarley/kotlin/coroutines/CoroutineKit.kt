package com.github.elifarley.kotlin.coroutines

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlin.coroutines.experimental.CoroutineContext

interface Actor<M> {
    val doRun: suspend kotlinx.coroutines.experimental.channels.ActorScope<M>.() -> Unit
}

class ActorOf<in M> private constructor(private val actor: ActorJob<M>) : ActorJob<M> by actor {
    companion object {
        operator fun <A : Actor<M>, M> invoke(
                klass: () -> A,
                context: CoroutineContext,
                capacity: Int = 0,
                start: CoroutineStart = CoroutineStart.DEFAULT
        ): ActorOf<M> =
                ActorOf(kotlinx.coroutines.experimental.channels.actor(context, capacity, start, klass().doRun))
    }

    suspend operator fun rem(msg: M) = actor.send(msg)
}

fun <T> delayWhile(
        context: CoroutineContext,
        predicate: (T.() -> Boolean) = {this == null},
        maxElapsedTime: Long = 2 * 60 * 1000,
        maxDelayPerIter: Long = 30000L,
        block: suspend () -> T
): Deferred<T> = async(context) {
    val start = System.currentTimeMillis()
    var result: T?
    while (true) {
        result = block()
        println("[delayWhile] ${System.currentTimeMillis() - start} ms; Predicate: ${result?.predicate() ?: true}") // TODO Remove
        if (!(result?.predicate() ?: true)) break
        if (System.currentTimeMillis() - start >= maxElapsedTime) {
            throw TimeoutException("Max elapsed time exceeded. Elapsed time: ${System.currentTimeMillis() - start}ms")
        }
        delay(Math.min(maxDelayPerIter, System.currentTimeMillis() - start))
    }
    result!!
}
