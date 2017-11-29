package com.orgecc.test

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.File
import java.util.*

fun ResponseEntity<*>.exportToMap() = linkedMapOf(
            "http-status" to statusCode.name,
            "body" to body.toString()
    )

fun ResponseEntity<*>.export(file: File, comments: String? = null) = Properties().also {
    it.putAll(this.exportToMap())
    it.store(file.outputStream(), comments)
}

fun toResponseEntity(m: Map<String, String>): ResponseEntity<String> = ResponseEntity(
        m["body"] ?: "",
        m["http-status"]?.let { HttpStatus.valueOf(it) } ?: HttpStatus.OK
)

inline fun callWithMockSupport(mockFilePropName: String, realCall: () -> ResponseEntity<String>): ResponseEntity<String> =
        callWithMockSupport(mockFilePropName, ::toResponseEntity, realCall)
