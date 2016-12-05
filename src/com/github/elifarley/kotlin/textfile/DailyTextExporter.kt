package com.orgecc.util.textfile

import com.orgecc.util.DateTimeKit.toDate
import com.orgecc.util.WithLogging
import com.orgecc.util.io.IOWithDigest
import com.orgecc.util.path.createParentDirectories
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

abstract class DailyTextExporter<out E: Any> (
        config: DailyTextExporterConfig<ResultSet, E>,
        dataSource: DataSource
        )
: DailyTextImex(config, dataSource) {

    companion object : WithLogging()

    init {
        LOG.info("[init] ${config.reportName}.listQuery: [{}]", config.listQuery)

        if (config.listQuery.isNullOrBlank()) throw IllegalArgumentException("'${config.reportName}.listQuery' is null or blank!")

        if (!config.listQuery.trim().contains(' ')) throw IllegalArgumentException("'${config.reportName}.listQuery' has no spaces!")

        config.listQuery.count { it == '?' }.let {
            if (it < 2) throw IllegalArgumentException("'${config.reportName}.listQuery' has '$it' parameters, but it should have at least 2")
        }

    }

    @Suppress("UNCHECKED_CAST")
    override val config: DailyTextExporterConfig<ResultSet, E>
        get() = super.config as DailyTextExporterConfig<ResultSet, E>

    private fun asResultSetHandler(objectHandler: (E) -> Unit): (rs: ResultSet) -> Unit =
            { it -> objectHandler(config.toObject(it)) }

    fun forEachRowOH(params: NextDailyImexParams, objectHandler: (E) -> Unit): Unit =
            forEachRow(params, asResultSetHandler(objectHandler))

    private fun forEachRow(params: NextDailyImexParams, rsHandler: (rs: ResultSet) -> Unit): Unit {

        LOG.info("[forEachRow] {}; listQuery SQL: [{}]", params, config.listQuery)
        jdbcTemplate.query(config.listQuery, params.queryParams, rsHandler)

    }

    private fun export(header: Map<String, Any>, params: NextDailyImexParams, mdWriter: IOWithDigest.MDWriter): Int {

        mdWriter.write(header)

        var detailLineCount = 0
        forEachRowOH(params) {
            mdWriter.write(it)
            detailLineCount++
            LOG.trace("#{}: {}", detailLineCount, it)
        }

        mdWriter.write(detailLineCount + 2)

        mdWriter.flush()
        return detailLineCount

    }

    fun export(outputFilePrefix: String, fileDate: LocalDateTime) {

        val processContext = Context().put("file-date", fileDate.toString())

        val params: NextDailyImexParams?
        try {
            val endDate = fileDate.toLocalDate().minusDays(1L) // end date is yesterday
            params = processContext.nextParamsForDate(endDate) ?: return

        } catch (e: Exception) {
            processContext.close("[export] Error in 'nextReportParams'", e)
            throw e
        }

        val fileSeq = params.nextSequence
        val header = mapOf("file-date" to fileDate.toDate(), "file-seq" to fileSeq)

        val filePath: Path
        try {
            filePath = Paths.get(outputFilePrefix.addDateTimeSequence(
                    header["file-date"] as Date,
                    header["file-seq"] as Int)
            ).createParentDirectories()

        } catch(e: Exception) {
            processContext.close("[export] Unable to create output file path for prefix [$outputFilePrefix]", e)
            throw e
        }

        processContext.startExport(filePath)

        try {

            var detailLineCount: Int = 0
            var md5 = ByteArray(0)

            config.useNewWriter(config.reportName, filePath) { mdWriter ->
                detailLineCount = export(header, params, mdWriter)
                md5 = mdWriter.digest
            }

            processContext.endIO(detailLineCount, md5)

        } catch(e: Exception) {
            processContext.close("[export] Unable to export data", e)
            throw e

        } finally {
            processContext.close()
        }

    }

}
