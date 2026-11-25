package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import java.io.File

internal class LocalGcReport(
    private val lifecycle: (String) -> Unit,
) {
    fun report(
        metrics: GCReportMetrics,
        logPath: String,
        buildOutput: File,
        histogramBucket: Bucket?,
    ) {
        val report = LocalReportProjection.project(logPath, metrics, histogramBucket)
        val csvFileName = logPath.getFileNameCsvLog()
        val csvFile = File(buildOutput, csvFileName)
        csvFile.parentFile.mkdirs()
        csvFile.writeText(report.collectionCsv)

        report.histogramCsv?.let { histogramCsv ->
            val histogramFile = File(buildOutput, "histogram_$csvFileName")
            histogramFile.parentFile.mkdirs()
            histogramFile.writeText(histogramCsv)
        }

        report.consoleTables.forEach(lifecycle)
    }
}
