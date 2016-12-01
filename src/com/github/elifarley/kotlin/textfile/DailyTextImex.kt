package com.orgecc.util.textfile

import com.orgecc.persistence.StreamingResultSetEnabledJdbcTemplate
import com.orgecc.util.MDCCloseable
import com.orgecc.util.WithLogging
import com.orgecc.util.concurrent.MaxFrequencyBarrier
import com.orgecc.util.stopIfRunning
import com.orgecc.util.toHexString
import net.logstash.logback.marker.Markers
import org.apache.commons.lang3.time.StopWatch
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

open class DailyTextImex(open val config: DailyTextImexConfig, dataSource: DataSource) {

    companion object : WithLogging() {
        val barrier = MaxFrequencyBarrier.newInstance(.9)
    }

    protected val jdbcTemplate: JdbcTemplate

    init {
        this.jdbcTemplate = StreamingResultSetEnabledJdbcTemplate(dataSource).apply {
            this.fetchSize = 1000
        }
    }

    protected val nextSeqQuery = "select top 1 LAST_SEQUENCE_NUMBER + 1 as nextSeq, dateadd(day, 1, LAST_DATE_PARAM) as nextDate" +
            " from REPORT_LOG where report_name='${config.reportName}' and LAST_SEQUENCE_NUMBER >= -1 order by CREATED desc -- REPORT_LOG"

    private val onSuccessSQL = "insert into REPORT_LOG" +
            " (REPORT_NAME, LAST_SEQUENCE_NUMBER, LAST_DATE_PARAM, MD5, ELAPSED_MILLIS, ITEM_COUNT)" +
            " values (?,?,?,?,?,?)"

    fun nextReportParams(endDate: LocalDate) = try {
        jdbcTemplate.queryForObject(nextSeqQuery) {
            rs: ResultSet, rowNum: Int ->
            Pair(rs.getInt(1), rs.getDate(2).toLocalDate())

        }

    } catch (erdae: EmptyResultDataAccessException) {
        Pair(1, LocalDate.ofYearDay(1970, 1))

    }.let {
        NextDailyImexParams(it.first, it.second, endDate)
    }

    fun endIO(type: Type, detailLineCount: Int, filePath: Path, mdc: MDCCloseable) {

        mdc.put("file-path", filePath.toString())

        val fileSize = Files.size(filePath)

        mdc.put("file-size", fileSize)

        val totalLineCount = detailLineCount + 2L
        val computedFileSize = totalLineCount * config.lineLength

        if (fileSize == computedFileSize) return

        if (fileSize < computedFileSize) throw IllegalArgumentException(
                "Actual file size ($fileSize) is ${computedFileSize - fileSize} bytes smaller than computed file size"
        )

        if (fileSize > computedFileSize + totalLineCount * 2) throw IllegalArgumentException(
                "Actual file size ($fileSize) is ${fileSize - (computedFileSize + totalLineCount * 2)} bytes bigger than computed file size and extra EOL"
        )

        if (type == Type.EXPORT) throw IllegalArgumentException(
                "Actual file size ($fileSize) is ${fileSize - computedFileSize} bytes bigger than computed file size"
        )

        LOG.warn("[endIO] Actual file size ($fileSize) is ${fileSize - computedFileSize} bytes bigger than computed file size")

    }

    fun insert(lastSequenceNumber: Int, lastDateParam: Date, md5: ByteArray, itemCount: Int, elapsedMillis: Int) {

        try {

            barrier.await()
            val updated = jdbcTemplate.update(
                    onSuccessSQL, config.reportName, lastSequenceNumber, lastDateParam, md5, elapsedMillis, itemCount
            )

            if (updated != 1) {
                LOG.error("[insert] Unexpected affected count: '$updated'; SQL: [$onSuccessSQL]")
            }

        } catch (e: Exception) {
            LOG.error("[insert] Error when trying to update REPORT_LOG table!", e)

        }

    }

    enum class Type { NONE, IMPORT, EXPORT }

    protected inner class Context() : Closeable, WithLogging() {

        private val mdc = MDCCloseable().put("report-name", this@DailyTextImex.config.reportName)
        private val stopWatch = StopWatch().apply { start() }
        private lateinit var params: NextDailyImexParams
        private lateinit var filePath: Path
        private var type: Type = Type.NONE

        fun put(key: String, value: Any?): Context { mdc.put(key, value); return this }

        fun nextParamsForDate(endDate: LocalDate): NextDailyImexParams? {
            params = this@DailyTextImex.nextReportParams(endDate)
            if (!checkParams(params)) return null
            return params
        }

        private fun checkParams(params: NextDailyImexParams): Boolean {

            val fileSeq = params.nextSequence
            mdc.put("file-seq", fileSeq)

            if (fileSeq <= 0) {
                stopIO()
                LOG.warn("[checkParams] Next file sequence is less than 1 ({}), process skipped.", fileSeq)
                close()
                return false
            }

            return true

        }

        fun startImport(filePath: Path) {
            start(Type.IMPORT, filePath)
        }

        fun startExport(filePath: Path) {
            start(Type.EXPORT, filePath)
        }

        fun start(type: Type, filePath: Path) {
            this.type = type
            this.filePath = filePath
            mdc.put("imex-type", type.name.toLowerCase())
            LOG.info("[start] {} started: '{}-{}'; File: [{}]", type, this@DailyTextImex.config.reportName, params.nextSequence, filePath.toString())
        }

        fun endIO(detailLineCount: Int, md5: ByteArray) {

            mdc.put("md5", md5.toHexString())

            if (detailLineCount <= 0) {
                LOG.warn("[endIO] Element collection is EMPTY!")
            }

            val elapsedMillis = stopIO()

            val ips = if (detailLineCount == 0) ""
            else "%.2f".format(Locale.ROOT, 1e3 * detailLineCount / elapsedMillis).let {
                mdc.put("items-per-second", it)
                " ($it ips)"
            }

            val marker = Markers.appendEntries(mapOf(
                    "start-date" to params.startDate.toString(),
                    "end-date" to params.endDate.toString(),
                    "total-line-count" to detailLineCount + 2,
                    "line-length" to this@DailyTextImex.config.lineLength
            ))

            try {
                this@DailyTextImex.endIO(type, detailLineCount, filePath, mdc)

            } catch (e: Exception) {

                LOG.error(marker, "[endIO] {} finished (with ERROR): '{}-{}'; {} data lines in {}{}; ERROR: {}",
                        type, this@DailyTextImex.config.reportName, params.nextSequence, detailLineCount, stopWatch, ips, e.message
                )

                throw e

            }

            LOG.info(marker, "[endIO] {} finished: '{}-{}'; {} data lines in {} ms = {}{}",
                    type, this@DailyTextImex.config.reportName, params.nextSequence, detailLineCount, elapsedMillis, stopWatch, ips
            )

            this@DailyTextImex.insert(params.nextSequence, params.queryParams[1] as Date, md5, detailLineCount, elapsedMillis)

        }

        fun stopIO(): Int {

            mdc.put("elapsed-time", stopWatch.stopIfRunning())

            val elapsedMillis = stopWatch.time.let {
                if (it <= Int.MAX_VALUE) it.toInt() else {
                    LOG.warn("[stop] Operation took too long to complete ({} ms).", it)
                    Int.MAX_VALUE
                }
            }

            mdc.put("elapsed-millis", elapsedMillis)

            return elapsedMillis

        }

        fun close(msg: String = "", e: Exception? = null) {
            stopIO()
            if (e != null) LOG.error(msg, e)
            else if (msg.isNotEmpty()) LOG.error(msg)
            mdc.close()
        }

        override fun close() {
            close("")
        }

    }

}
