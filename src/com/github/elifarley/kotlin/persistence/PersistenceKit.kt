package com.orgecc.util.persistence

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

fun toConvertedArrayParams(params: Collection<Any?>) = params.toTypedArray().apply {
    this.forEachIndexed { i, element ->
        when (element) {
            is LocalDate -> this[i] = java.sql.Date.valueOf(element)
            is LocalDateTime -> this[i] = java.sql.Timestamp.valueOf(element)
        }
    }
}
