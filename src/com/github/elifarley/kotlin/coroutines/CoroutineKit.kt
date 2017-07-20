package com.github.elifarley.kotlin.coroutines

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlin.coroutines.experimental.CoroutineContext

fun <T> delayWhile(
        predicate: T.() -> Boolean,
        maxDelayPerItem: Long = 30000L,
        context: CoroutineContext,
        block: suspend () -> T
): Deferred<T> = async(context) {
    val start = System.currentTimeMillis()
    var result : T?
    while (true) {
        result = block()
        println("[delayWhile] ${System.currentTimeMillis() - start} ms; Predicate: ${result?.predicate() ?: true}") // TODO Remove
        if (! (result?.predicate() ?: true)) break
        delay(Math.min(maxDelayPerItem, System.currentTimeMillis() - start))
    }
    result!!
}
