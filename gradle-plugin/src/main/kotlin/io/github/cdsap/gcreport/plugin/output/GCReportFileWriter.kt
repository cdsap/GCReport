package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.gradle.api.logging.Logging
import java.io.File

internal class GCReportFileWriter(
    private val buildOutput: File,
    histogramEnabled: Boolean,
    histogramBucket: Bucket,
) {
    private val logger = Logging.getLogger(GCReportFileWriter::class.java)
    private val renderer =
        GCReportRenderer(
            GCReportRenderingOptions(histogramEnabled, histogramBucket),
        )

    fun write(
        logFile: File,
        metrics: GCReportMetrics,
    ) {
        val report = renderer.render(logFile.path, metrics)
        val csvFile = File(buildOutput, logFile.path.getFileNameCsvLog())
        csvFile.parentFile.mkdirs()
        csvFile.writeText(report.collectionCsv)

        report.histogramCsv?.let { histogramCsv ->
            val histogramFile = File(buildOutput, "histogram_${logFile.path.getFileNameCsvLog()}")
            histogramFile.parentFile.mkdirs()
            histogramFile.writeText(histogramCsv)
        }

        report.consoleReports.forEach(logger::lifecycle)
    }
}
