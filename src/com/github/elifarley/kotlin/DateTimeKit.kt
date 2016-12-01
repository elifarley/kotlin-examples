package com.orgecc.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

/**
 * Created by elifarley on 31/08/16.
 */

object DateTimeKit {

    fun toZoneOffset(localDateTime: LocalDateTime, zoneId: ZoneId? = null): ZoneOffset {

        val lzid = zoneId ?: ZoneId.systemDefault()

        val trans = lzid.rules.getTransition(localDateTime) ?:
                // Normal case: only one valid offset
                return lzid.rules.getOffset(localDateTime)

        // Gap or Overlap: determine what to do from transition
        logger(DateTimeKit.javaClass).warn("[toZoneOffset] {}", trans.toString(), RuntimeException("Timezone transition found"))

        return lzid.rules.getOffset(localDateTime)

    }

    @JvmOverloads
    fun TemporalAccessor.toDate(zoneId: ZoneId? = null) = LocalDateTime.from(this).toDate(zoneId)

    @JvmOverloads
    fun LocalDate.toDate(zoneId: ZoneId? = null) = this.atStartOfDay().toDate(zoneId)

    @JvmOverloads
    fun LocalDateTime.toDate(zoneId: ZoneId? = null): Date {
        return Date.from(this.toInstant(toZoneOffset(this, zoneId)))!!
    }

    @JvmOverloads
    fun Instant.toLocalDateTime(zoneId: ZoneId? = null) = LocalDateTime.ofInstant(this, zoneId ?: ZoneId.systemDefault())!!

    @JvmOverloads
    fun Date.toLocalDateTime(zoneId: ZoneId? = null) = when {
        // java.sql.Date doesn't have time information, so we need to create a java.util.Date instance out of it
        this is java.sql.Date -> Date(this.time).toInstant().toLocalDateTime(zoneId)
        else -> this.toInstant().toLocalDateTime(zoneId)
    }

    val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneId.systemDefault())!!
    fun DateTimeFormatter.format(date: Date) = this.format(date.toInstant())!!

}
