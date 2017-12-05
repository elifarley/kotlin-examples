package com.orgecc.test

import com.orgecc.logging.WithLogging
import java.io.File
import java.io.FileInputStream
import java.util.*

abstract class MockableCall<R> {
    abstract fun call(realCall: () -> R): R?
    operator inline fun invoke(noinline realCall: () -> R): R? = call(realCall)
}

interface MockableCallResult<R> {
    var mockResult: R?
    fun resetMockResult()
    fun expectCall(result: R? = null, expectedInitialCallCount: Int = 0, expectedCallCount: Int = 1, block: () -> Unit): Unit
    operator fun invoke(result: R? = null, expectedInitialCallCount: Int = 0, expectedCallCount: Int = 1, block: () -> Unit)
            = expectCall(result, expectedInitialCallCount, expectedCallCount, block)
}

interface CountingValue<T> {
    var value: T?
    val valueIsSet: Boolean
    val readCount: Int
    fun reset()
}

class CountingValueImpl<T> : CountingValue<T> {

    override var readCount = 0
        get() = synchronized(field) {
            field
        }
        private set

    override var valueIsSet = false
        private set

    override var value: T? = null
        get() = synchronized(readCount) {
            readCount++
            field
        }
        set(v) = synchronized(readCount) {
            field = v
            valueIsSet = true
            // readCount = 0
        }

    override fun reset() = synchronized(readCount) {
        value = null
        valueIsSet = false
        readCount = 0
    }

}

abstract class MockableCallHelper<R>(private val mockFilePropName: String) : MockableCall<R>(), MockableCallResult<R> {

    companion object : WithLogging()

    inline val mockableCall: MockableCall<R> get() = this
    inline val mockableCallResult: MockableCallResult<R> get() = this

    private val mockCV = CountingValueImpl<R>()

    override var mockResult: R?
        get() = mockCV.value
        set(v) {
            mockCV.value = v
        }

    override fun resetMockResult() {
        mockCV.reset()
    }

    protected abstract val importResultFromMapFun: Map<String, String>.() -> R

    override fun expectCall(result: R?, expectedInitialCallCount: Int, expectedCallCount: Int, block: () -> Unit) = try {
        if (expectedInitialCallCount != mockCV.readCount) {
            throw IllegalStateException("Expected $expectedInitialCallCount as initial call count, but got ${mockCV.readCount}")
        }
        if (result != null) mockCV.value = result
        block()
        if (mockCV.readCount != expectedInitialCallCount + expectedCallCount) {
            throw IllegalStateException("[MOCK: $mockFilePropName] Expected call count: $expectedCallCount; actual: ${mockCV.readCount}")
        }
        Unit

    } finally {
        if (result != null) resetMockResult()
    }

    override fun call(realCall: () -> R): R? =
            if (mockCV.valueIsSet) {
                mockResult.also {
                    LOG.error("[MOCK #${mockCV.readCount}: $mockFilePropName] Returning object: {}", it)
                }

            } else System.getProperty(mockFilePropName, System.getenv(mockFilePropName)).let { mockFilePath ->

                if (mockFilePath.isNullOrEmpty()) {
                    return realCall()
                }

                LOG.error("[MOCK: $mockFilePropName] Reading mocked response from '{}'...", mockFilePath)
                importResultFromFile(mockFilePath)
            }

    private fun importResultFromFile(mockFilePath: String?): R = File(mockFilePath).let { mockFile ->
        try {

            if (!mockFile.canRead()) {
                throw IllegalArgumentException("File '${mockFile.canonicalPath}' cannot be read.")
            }

            Properties().let {
                it.load(FileInputStream(mockFile))
                importResultFromMapFun(it as Map<String, String>)
            }

        } catch (e: IllegalArgumentException) {
            throw e

        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to process '${mockFile.path}': $e", e)
        }

    } // File(mockFilePath)

}
