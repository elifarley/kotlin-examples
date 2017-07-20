package com.github.elifarley.kotlin.coroutines

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
        println("[delayWhile] Predicate: ${result?.predicate() ?: true}")
        if (! (result?.predicate() ?: true)) break
        println("[delayWhile] ${System.currentTimeMillis() - start}") // TODO Remove
        delay(Math.min(maxDelayPerItem, System.currentTimeMillis() - start))
    }
    result!!
}
