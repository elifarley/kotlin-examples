package com.orgecc.util.textfile

import com.orgecc.util.DateTimeKit
import com.orgecc.util.DateTimeKit.format
import com.orgecc.util.io.IOWithDigest.MDReader
import com.orgecc.util.io.IOWithDigest.MDWriter
import com.orgecc.util.logger
import com.orgecc.util.persistence.toConvertedArrayParams
import java.nio.file.Path
import java.time.LocalDate
import java.util.*

/**
 * Created by elifarley on 25/11/16.
 */
fun String.addDateTimeSequence(date: Date, seq: Int) = this +
        "${DateTimeKit.DATE_FORMATTER.format(date)}-$seq.txt"

open class DailyTextImexConfig(val reportName: String, val lineLength: Int)

abstract class DailyTextImporterConfig(
        reportName: String,
        lineLength: Int,
        val useNewReader: (streamName: String, filePath: Path, block: (MDReader) -> Unit) -> Unit
): DailyTextImexConfig(reportName,lineLength)

abstract class DailyTextExporterConfig<in I: Any, out E: Any>(
        reportName: String,
        lineLength: Int,
        val listQuery: String,
        val useNewWriter: (streamName: String, filePath: Path, block: (MDWriter) -> Unit) -> Unit
): DailyTextImexConfig(reportName,lineLength) {

    abstract fun toObject(rs: I): E

}

data class NextDailyImexParams(val nextSequence: Int, val startDate: LocalDate, val endDate: LocalDate) {

    val queryParams: Array<Any?> = toConvertedArrayParams(listOf(startDate, endDate))

    init {

        if (startDate != endDate) {
            val deltaInDays = (endDate.toEpochDay() - startDate.toEpochDay()).toInt()
            val log = logger(this.javaClass)
            when {
                deltaInDays > 0 -> log.warn("[init] {} days in range [{}, {}] ", deltaInDays + 1, startDate, endDate)
                deltaInDays < 0 -> log.error("[init] {} days in NEGATIVE range [{}, {}]", -deltaInDays + 1, startDate, endDate)
            }
        }

    }

}
