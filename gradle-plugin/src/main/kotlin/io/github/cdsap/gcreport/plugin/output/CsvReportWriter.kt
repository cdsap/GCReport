package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import io.github.cdsap.gcreport.plugin.report.GCReportFormatter
import java.io.File

internal class CsvReportWriter(
    private val buildOutput: File,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
) {
    private val formatter = GCReportFormatter(histogramBucket)

    fun write(
        logFile: File,
        metrics: GCReportMetrics,
    ) {
        val csvFile = File(buildOutput, logFile.path.getFileNameCsvLog())
        csvFile.parentFile.mkdirs()
        csvFile.writeText(formatter.collectionCounts(metrics))

        if (histogramEnabled) {
            val histogramFile = File(buildOutput, "histogram_${logFile.path.getFileNameCsvLog()}")
            histogramFile.parentFile.mkdirs()
            histogramFile.writeText(formatter.histogram(metrics))
        }
    }
}
