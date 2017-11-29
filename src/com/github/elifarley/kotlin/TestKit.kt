package com.orgecc.test

import com.orgecc.logging.logger
import java.io.File
import java.io.FileInputStream
import java.util.*

abstract class MockFileHelper<in T>(private val mockFilePropName: String) {

    private var mockTempFile: File? = null

    abstract val exportToMapFun: T.() -> Map<String, String>

    fun setUp(mockResponse: T? = null) {
        mockTempFile?.delete()
        mockTempFile = mockResponse?.exportToMapFun()?.exportToTempFile()?.also { tmpFile ->
            System.setProperty(mockFilePropName, tmpFile.canonicalPath)
        }
    }

    fun tearDown() {
        mockTempFile?.delete()
        System.setProperty(mockFilePropName, "")
    }
}

fun Map<String, String>.exportToTempFile(comments: String? = null) = createTempFile(suffix = ".properties").also { tmp ->
    val props = if (this is Properties)
        this
    else
        Properties().also { newProps ->
            newProps.putAll(this)
        }
    props.store(tmp.outputStream(), comments)
}

fun <T> T.exportToTempFile(exportToMap: T.() -> Map<String, String>, comments: String? = null) =
        this.exportToMap().exportToTempFile(comments)

inline fun <R> callWithMockSupport(mockFilePropName: String, noinline mockCall: Map<String, String>.() -> R, realCall: () -> R): R {

    val mockFilePath : String? = System.getProperty(mockFilePropName, System.getenv(mockFilePropName))
    if (mockFilePath.isNullOrEmpty()) {
        return realCall()
    }

    logger(mockCall::class).error("[MOCK] Reading mocked response from '{}'...", mockFilePath)
    return File(mockFilePath).let { mockFile ->
        try {

            if (!mockFile.canRead()) {
                throw IllegalArgumentException("File '${mockFile.canonicalPath}' cannot be read.")
            }

            Properties().let {
                it.load(FileInputStream(mockFile))
                mockCall(it as Map<String, String>)
            }

        } catch (e: IllegalArgumentException) {
            throw e

        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to process '${mockFile.path}': $e", e)
        }

    } // File(mockFilePath)

}
