package com.orgecc.textfile

import com.orgecc.util.StreamingResultSetEnabledJdbcTemplate
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Date
import javax.sql.DataSource

open class ReportLogHandler(open val config: ReportLogConfig, dataSource: DataSource) {

    companion object : WithLogging()

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

    fun insert(lastSequenceNumber: Int, lastDateParam: Date, md5: String, itemCount: Int, elapsedMillis: Int) {

        val updated = jdbcTemplate.update(
                onSuccessSQL, config.reportName, lastSequenceNumber, lastDateParam, md5, elapsedMillis, itemCount
        )

        if (updated != 1) {
            throw IllegalArgumentException("Unexpected affected count: '$updated'; SQL: [$onSuccessSQL]")
        }

    }

}
