package io.github.cdsap.gcreport.plugin.output

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
    private val csvWriter = CsvReportWriter(buildOutput, histogramEnabled, histogramBucket)
    private val consoleReport = ConsoleReport(histogramEnabled, histogramBucket)

    fun write(
        logFile: File,
        metrics: GCReportMetrics,
    ) {
        csvWriter.write(logFile, metrics)
        consoleReport.render(logFile.path, metrics).forEach(logger::lifecycle)
    }
}
