fun <T> Array<out T>.toPairs() = if (isEmpty()) emptyArray() else Array(this.size shr 1) { i ->
    Pair(this[i * 2], this[i * 2 + 1])
}
