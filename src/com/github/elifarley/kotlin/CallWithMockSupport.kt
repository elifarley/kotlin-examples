package com.orgecc.test

import com.orgecc.logging.WithLogging
import java.io.File
import java.io.FileInputStream
import java.util.*

abstract class CallWithMockSupport<R>(private val mockFilePropName: String) {

    companion object : WithLogging()

    var mockIsSet = false
        private set
    var mockResult: R? = null
        set(value) {
            field = value
            mockIsSet = true
        }
    fun resetMockResult() {
        mockResult = null
        mockIsSet = false
    }

    protected abstract val importResultFromMapFun: Map<String, String>.() -> R

    fun call(realCall: () -> R): R? =
            if (mockIsSet) {
                LOG.error("[MOCK] Returning object: {}", mockResult)
                mockResult

            } else System.getProperty(mockFilePropName, System.getenv(mockFilePropName)).let {mockFilePath ->

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
