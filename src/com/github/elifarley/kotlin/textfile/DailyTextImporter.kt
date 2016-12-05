package com.orgecc.util.textfile

import com.orgecc.util.WithLogging
import com.orgecc.util.io.IOWithDigest
import java.nio.file.Path
import java.time.LocalDate
import java.time.temporal.TemporalAccessor
import javax.sql.DataSource

abstract class DailyTextImporter(
        config: DailyTextImporterConfig,
        dataSource: DataSource
        )
: DailyTextImex(config, dataSource) {

    companion object : WithLogging()

    override val config: DailyTextImporterConfig
        get() = super.config as DailyTextImporterConfig

    fun checkHeader(params: NextDailyImexParams, header: Map<String, *>) {

        LOG.info("[checkHeader] {}", header.toString())

        (header.getOrElse("file-seq", { null }) as Int?) ?.let { actualFileSeq ->
            if (params.nextSequence != actualFileSeq) {
                throw IllegalArgumentException("[checkHeader] Expected file-seq: '${params.nextSequence}'; Actual: '$actualFileSeq'")
            }
        }

        val fileDate = (header.getOrElse("file-date", { null }) as TemporalAccessor?)?.let {
            LocalDate.from(it)
        } ?: LocalDate.MIN

        if (fileDate !in params.startDate .. params.endDate) {
            LOG.warn("[checkHeader] File date '$fileDate' is outside expected range '${params.startDate}' -> '${params.endDate}'.")
        }

    }

    fun checkTrailer(detailLineCount: Int, totalLines: Int) {
        if (totalLines - config.fixedLineCount != detailLineCount) throw RuntimeException(
                "Expected detail count: ${totalLines - config.fixedLineCount};\n" +
                        "Actual: $detailLineCount;\n")
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified E> forEachLine(params: NextDailyImexParams, mdReader: IOWithDigest.MDReader, objectImporter: (E) -> Unit): Int {

        var detailLineCount = 0
        var errorCount = 0
        var nonDetailLines = 0

        while (true) {

            val obj: Any = mdReader.read() ?: break

            when (obj) {

                is Map<*, *> -> {
                    nonDetailLines++
                    checkHeader(params, obj as Map<String, *>)
                }

                is E -> {
                    detailLineCount++
                    try {
                        objectImporter(obj)

                    } catch(e: Exception) {
                        errorCount++
                        LOG.error("[forEachLine] Error at line #$detailLineCount ($obj)", e)
                    }
                }

                is Int -> {
                    nonDetailLines++
                    checkTrailer(detailLineCount, obj)
                }

            }

        }

        if (errorCount > 0) throw IllegalArgumentException(
                "$errorCount lines caused errors; ${detailLineCount - errorCount} detail lines were processed normally."
        )

        if (nonDetailLines == 0) throw IllegalArgumentException(
                "No non-detail lines found."
        )

        return detailLineCount

    }

    inline fun <reified E> import(path: Path, endDate: LocalDate, crossinline objectImporter: (E) -> Unit) {

        val processContext = Context()

        try {

            val params = processContext.nextParamsForDate(endDate) ?: return

            processContext.startImport(path)

            var detailLineCount: Int = 0
            var md5 = ByteArray(0)

            config.useNewReader(config.reportName, path) { mdReader ->
                detailLineCount = forEachLine(params, mdReader, objectImporter)
                md5 = mdReader.digest
            }

            processContext.endIO(detailLineCount, md5)

        } catch(e: Exception) {
            processContext.close("[import] Unable to import data; ERROR: ${e.message}")
            throw e

        } finally {
            processContext.close()
        }

    }

}

