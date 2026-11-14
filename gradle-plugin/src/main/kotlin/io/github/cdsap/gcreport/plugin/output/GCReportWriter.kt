package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import java.io.File

internal class GCReportWriter(
    private val outputDirectory: File,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
    private val log: (String) -> Unit,
) {
    fun write(
        logPath: String,
        entries: List<GCEntry>,
    ) {
        val metrics = GCReportMetrics.from(entries)
        val histogram =
            if (histogramEnabled) {
                LocalReportOutput.Histogram(
                    histogramBucket,
                    Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram()),
                )
            } else {
                null
            }
        val report = LocalReportOutput.render(logPath, metrics, histogram)
        val csvFile = File(outputDirectory, logPath.getFileNameCsvLog())
        csvFile.parentFile.mkdirs()
        csvFile.writeText(report.collectionCsv)

        report.histogramCsv?.let { histogramCsv ->
            val histogramFile = File(outputDirectory, "histogram_${logPath.getFileNameCsvLog()}")
            histogramFile.parentFile.mkdirs()
            histogramFile.writeText(histogramCsv)
        }

        report.consoleTables.forEach(log)
    }
}
