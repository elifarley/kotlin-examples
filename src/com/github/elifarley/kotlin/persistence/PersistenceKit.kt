package com.orgecc.persistence

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

fun toConvertedArrayParams(params: Collection<Any?>) = params.toTypedArray().apply {
    this.forEachIndexed { i, element ->
        when (element) {
            is LocalDate -> this[i] = Date.valueOf(element)
            is LocalDateTime -> this[i] = Timestamp.valueOf(element)
        }
    }
}
