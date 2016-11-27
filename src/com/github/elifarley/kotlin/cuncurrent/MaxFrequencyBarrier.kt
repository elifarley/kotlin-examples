class MaxFrequencyBarrier(itemsPerSecond: Int = 1) {

    companion object {

        var lastMillis = System.currentTimeMillis()

        // TODO Return max items that can be processed
        @Synchronized
        fun await(millisStep: Int, itemCount: Int = 1): Unit {

            val now = System.currentTimeMillis()
            val toSleep = lastMillis - now

            if (toSleep > 0) {
                lastMillis = now + toSleep + millisStep * itemCount
                Thread.sleep(toSleep.toLong())

            } else {
                lastMillis = now + millisStep * itemCount

            }

        }
        
    }

    val millisStep: Int

    init {
        millisStep = 1000 / itemsPerSecond
    }

    fun await(itemCount: Int = 1) = MaxFrequencyBarrier.await(millisStep, itemCount)

}
