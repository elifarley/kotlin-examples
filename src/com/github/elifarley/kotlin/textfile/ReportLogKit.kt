package com.orgecc.textfile

import com.orgecc.utils.DateTimeKit.format
import java.nio.file.Path
import java.sql.ResultSet
import java.time.LocalDate

/**
 * Created by elifarley on 25/11/16.
 */
fun String.addDateTimeSequence(date: java.util.Date, seq: Int) = this +
        "${DateTimeKit.DATE_FORMATTER.format(date)}-$seq.txt"

open class ReportLogConfig(val reportName: String, val lineLength: Int)

abstract class ReportLogDBReaderConfig<out E: Any>(
        reportName: String,
        lineLength: Int,
        val listQuery: String,
        val useNewWriter: (streamName: String, filePath: Path, block: (WriterWithDigest) -> Unit) -> Unit
): ReportLogConfig(reportName,lineLength) {

    abstract fun toObject(rs: ResultSet): E

}

data class NextDailyReportParams(val nextSequence: Int, val startDate: LocalDate, val endDate: LocalDate) {

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
