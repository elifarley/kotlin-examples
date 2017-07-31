package com.orgecc.lib.http

import lib.collections.toPairs
import java.util.*

data class Headers(val pairs: Array<Pair<String, String>>) : List<Pair<String, String>> {

    constructor(vararg stringPairs: String) : this(stringPairs.toPairs())
    constructor(p: Collection<Pair<String, String>>) : this(p.toTypedArray())

    override val size = pairs.size
    override fun contains(element: Pair<String, String>) = pairs.contains(element)
    override fun containsAll(elements: Collection<Pair<String, String>>) = elements.all { contains(it) }
    override fun isEmpty() = pairs.isEmpty()
    override fun iterator() = pairs.iterator()
    // Interface List
    override fun get(index: Int) = pairs[index]

    override fun indexOf(element: Pair<String, String>) = pairs.indexOf(element)
    override fun lastIndexOf(element: Pair<String, String>) = pairs.lastIndexOf(element)
    override fun listIterator(): ListIterator<Pair<String, String>> = TODO("not implemented")
    override fun listIterator(index: Int): ListIterator<Pair<String, String>> = TODO("not implemented")
    override fun subList(fromIndex: Int, toIndex: Int): List<Pair<String, String>> = TODO("not implemented")

    class Builder {

        private val result = mutableListOf<Pair<String, String>>()

        fun add(name: String, value: String) = result.run {
            add(Pair(name, value))
            this@Builder
        }

        fun add(vararg stringPairs: String) = result.run {
            addAll(stringPairs.toPairs())
            this@Builder
        }

        fun build() = Headers(result)

    }

    override fun toString(): String {
        return pairs.joinToString { "${it.first}: ${it.second}" }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Headers) return false
        return Arrays.deepEquals(this.pairs, other.pairs)
    }

    override fun hashCode() = Arrays.deepHashCode(this.pairs)

}

fun main(args: Array<String>) {
    Headers("a", "b", "cx", "d").let { println(it) }
}
