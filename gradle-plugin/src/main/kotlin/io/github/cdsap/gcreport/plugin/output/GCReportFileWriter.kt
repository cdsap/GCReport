package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.gradle.api.logging.Logging
import java.io.File

internal class GCReportFileWriter(
    private val buildOutput: File,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
) {
    private val logger = Logging.getLogger(GCReportFileWriter::class.java)

    fun write(
        logFile: File,
        metrics: GCReportMetrics,
    ) {
        val histogram =
            if (histogramEnabled) {
                LocalReportOutput.Histogram(
                    histogramBucket,
                    Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram()),
                )
            } else {
                null
            }
        val report = LocalReportOutput.render(logFile.path, metrics, histogram)
        val csvFile = File(buildOutput, logFile.path.getFileNameCsvLog())
        csvFile.parentFile.mkdirs()
        csvFile.writeText(report.collectionCsv)

        report.histogramCsv?.let { histogramCsv ->
            val histogramFile = File(buildOutput, "histogram_${logFile.path.getFileNameCsvLog()}")
            histogramFile.parentFile.mkdirs()
            histogramFile.writeText(histogramCsv)
        }

        report.consoleTables.forEach(logger::lifecycle)
    }
}
