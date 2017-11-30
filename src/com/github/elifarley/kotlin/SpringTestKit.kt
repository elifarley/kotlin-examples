package com.orgecc.test

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun ResponseEntity<*>.exportToMap() = linkedMapOf(
            "http-status" to statusCode.name,
            "body" to body.toString()
    )

fun toResponseEntity(m: Map<String, String>): ResponseEntity<String> = ResponseEntity(
        m["body"] ?: "",
        m["http-status"]?.let { HttpStatus.valueOf(it) } ?: HttpStatus.OK
)

open class CallWithMockSupportForResponseEntity(mockFilePropName: String): CallWithMockSupport<ResponseEntity<String>>(mockFilePropName) {
    final override val importResultFromMapFun = ::toResponseEntity
}
