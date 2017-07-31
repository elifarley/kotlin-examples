package com.orgecc.lib.collections

/**
 * Created by ecc on 7/30/17.
 */

fun <T> Array<out T>.toPairs() = if (isEmpty()) emptyArray() else Array(this.size shr 1) {
    Pair(this[it * 2], this[it * 2 + 1])
}
