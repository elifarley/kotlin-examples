package com.orgecc.textfile

import com.orgecc.utils.DateTimeKit.toDate
import net.logstash.logback.marker.Markers
import org.apache.commons.lang3.time.StopWatch
import org.springframework.dao.EmptyResultDataAccessException
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

abstract class ReportLogHandlerDBReader<out E: Any> (
        config: ReportLogDBReaderConfig<ResultSet, E>,
        dataSource: DataSource
        )
: ReportLogHandler(config, dataSource) {

    companion object : WithLogging()

    init {
        LOG.info("[init] ${config.reportName}.listQuery: [{}]", config.listQuery)

        if (config.listQuery.isNullOrBlank()) throw IllegalArgumentException("'${config.reportName}.listQuery' is null or blank!")

        if (!config.listQuery.trim().contains(' ')) throw IllegalArgumentException("'${config.reportName}.listQuery' has no spaces!")

        config.listQuery.count { it == '?' }.let {
            if (it < 2) throw IllegalArgumentException("'${config.reportName}.listQuery' has '$it' parameters, but it should have at least 2")
        }

    }

    override val config: ReportLogDBReaderConfig<ResultSet, E>
        get() = super.config as ReportLogDBReaderConfig<ResultSet, E>

    private fun asResultSetHandler(objectHandler: (E) -> Unit): (rs: ResultSet) -> Unit =
            { it -> objectHandler(config.toObject(it)) }

    fun forEachRowOH(params: NextDailyReportParams, objectHandler: (E) -> Unit): Unit =
            forEachRow(params, asResultSetHandler(objectHandler))

    private fun forEachRow(params: NextDailyReportParams, rsHandler: (rs: ResultSet) -> Unit): Unit {

        LOG.info("[forEachRow] {}; listQuery SQL: [{}]", params, config.listQuery)
        jdbcTemplate.query(config.listQuery, params.queryParams, rsHandler)

    }

    fun nextReportParams(endDate: LocalDate) = try {
        jdbcTemplate.queryForObject(nextSeqQuery) {
            rs: ResultSet, rowNum: Int ->
            Pair(rs.getInt(1), rs.getDate(2).toLocalDate())

        }

    } catch (erdae: EmptyResultDataAccessException) {
        Pair(1, LocalDate.ofYearDay(1970, 1))

    }.let {
        NextDailyReportParams(it.first, it.second, endDate)
    }

    fun report(outputFilePrefix: String, fileDate: LocalDateTime) {

        val stopWatch = StopWatch().apply { start() }

        val mdc = MDCCloseable().put("report-name", config.reportName).put("file-date", fileDate.toString())

        val params: NextDailyReportParams

        val fileSeq: Int
        try {
            params = nextReportParams(fileDate.toLocalDate().minusDays(1L)) // end date is yesterday
            fileSeq = params.nextSequence
            mdc.put("file-seq", fileSeq)

            if (fileSeq <= 0) {
                mdc.put("elapsed-time", stopWatch.stopIfRunning())
                LOG.warn("[report] Next file sequence is less than 1 ({}), report skipped.", fileSeq)
                mdc.close()
                return
            }

        } catch (e: Exception) {
            mdc.put("elapsed-time", stopWatch.stopIfRunning())
            LOG.error("[report] Error in 'nextReportParams'", e)
            mdc.close()
            return
        }

        val header = mapOf("file-date" to fileDate.toDate(), "file-seq" to fileSeq)

        val outputFile: Path
        try {
            outputFile = Paths.get(outputFilePrefix.addDateTimeSequence(
                    header["file-date"] as java.util.Date,
                    header["file-seq"] as Int)
            ).createParentDirectories()

        } catch(e: Exception) {
            mdc.put("elapsed-time", stopWatch.stopIfRunning())
            LOG.error("[report] Unable to create output file path for prefix [{}]", outputFilePrefix, e)
            mdc.close()
            return
        }

        try {
            config.useNewWriter(config.reportName, outputFile) {
                writerWithDigest ->

                LOG.info("[report] Started '{}-{}'. output-file: [{}]", config.reportName, fileSeq, outputFile.toString())
                header.let {
                    writerWithDigest.write(it)
                }

                var detailLineCount = 0
                forEachRowOH(params) {
                    writerWithDigest.write(it)
                    detailLineCount++
                    LOG.trace("#{}: {}", detailLineCount, it)
                }

                writerWithDigest.write(detailLineCount + 2)

                if (detailLineCount <= 0) {
                    LOG.warn("[report] Result set is EMPTY!")
                }

                writerWithDigest.flush()

                val md5 = writerWithDigest.digest

                val outputFileSize: Long = try {
                    Files.size(outputFile)

                } catch (e: Exception) {
                    LOG.error("[report] Unable to get file size! output-file: [{}]", outputFile.toString(), e)
                    -1
                }

                val elapsedMillis = stopWatch.stopIfRunning().time.let {
                    if (it <= Int.MAX_VALUE) it.toInt() else {
                        LOG.warn("[report] Operation took too long to complete ({} ms).", it)
                        Int.MAX_VALUE
                    }
                }

                mdc.put("elapsed-time", stopWatch)
                mdc.put("elapsed-millis", elapsedMillis)
                val ips = if (detailLineCount == 0) ""
                else "%.2f".format(Locale.ROOT, 1e3 * detailLineCount / elapsedMillis).let {
                    mdc.put("items-per-second", it)
                    " ($it ips)"
                }

                val marker = Markers.appendEntries(mapOf(
                        "report-start-date" to params.startDate.toString(),
                        "report-end-date" to params.endDate.toString(),
                        "output-file" to outputFile.toString(),
                        "total-line-count" to detailLineCount + 2,
                        "file-size" to outputFileSize,
                        "md5" to md5.toHexString(),
                        "line-length" to config.lineLength
                ))

                val computedLineCount = BigDecimal(outputFileSize.toString()).div(BigDecimal.valueOf(config.lineLength.toLong())).stripTrailingZeros()

                if (outputFileSize >= 0 && computedLineCount != BigDecimal.valueOf(detailLineCount + 2L)) {
                    LOG.error(marker, "[report] Computed line count ({}) doesn't match actual line count ({})!", computedLineCount, detailLineCount + 2)
                    LOG.error(marker, "[report] Finished (with ERROR) '{}-{}': {} data lines in {}{}",
                            config.reportName, fileSeq, detailLineCount, stopWatch, ips
                    )

                } else {

                    LOG.info(marker, "[report] Finished '{}-{}': {} data lines in {} ms = {}{}",
                            config.reportName, fileSeq, detailLineCount, elapsedMillis, stopWatch, ips
                    )

                    try {
                        insert(fileSeq, params.queryParams[1] as Date, md5, detailLineCount, elapsedMillis)

                    } catch (e: Exception) {
                        LOG.error("[report] Error when trying to update REPORT_LOG table!", e)

                    }

                }

            } // BeanIOKit.useNewWriter

        } catch(e: Exception) {
            mdc.put("elapsed-time", stopWatch.stopIfRunning())
            LOG.error("[report] Unable to handle data", e)

        } finally {
            mdc.close()
        }

    }

}

