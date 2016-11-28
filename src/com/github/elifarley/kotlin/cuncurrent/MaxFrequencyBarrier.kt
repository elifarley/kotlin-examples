package com.orgecc.concurrent

class MaxFrequencyBarrier
private constructor (itemsPerSecond: Double) {

    companion object {

        fun newInstance(itemsPerSecond: Double = 1e0) = MaxFrequencyBarrier(itemsPerSecond)

        var lastMillis = System.currentTimeMillis()

        // TODO Return max items that can be processed
        @Synchronized
        fun await(millisStep: Int, itemCount: Int = 1): Unit {

            val now = System.currentTimeMillis()
            val toSleep = lastMillis - now

            if (toSleep > 0) {
                lastMillis += millisStep * itemCount
                Thread.sleep(toSleep.toLong())

            } else {
                lastMillis = now + millisStep * itemCount

            }

        }

    }

    val millisStep: Int

    init {
        millisStep = (1000 / itemsPerSecond).toInt()
    }

    fun await(itemCount: Int = 1) = MaxFrequencyBarrier.await(millisStep, itemCount)

}
