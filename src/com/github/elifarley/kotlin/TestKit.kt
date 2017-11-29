package com.orgecc.test

import com.orgecc.logging.logger
import java.io.File
import java.io.FileInputStream
import java.util.*

inline fun <R> call(mockFilePropName: String, noinline mockCall: (Map<String, String>) -> R, realCall: () -> R): R {

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

