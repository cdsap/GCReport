package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import java.io.File

internal class GCReportCsvWriter(
    private val buildOutput: File,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
) {
    fun write(
        logFile: File,
        metrics: GCReportMetrics,
    ) {
        val csvFile = File(buildOutput, logFile.path.getFileNameCsvLog())
        csvFile.parentFile.mkdirs()
        csvFile.writeText(
            buildString {
                appendLine("Collection type,Occurrences")
                metrics.collectionTypeCounts().forEach { (description, count) ->
                    appendLine("$description,$count")
                }
            },
        )

        if (histogramEnabled) {
            val histogramFile = File(buildOutput, "histogram_${logFile.path.getFileNameCsvLog()}")
            histogramFile.parentFile.mkdirs()
            histogramFile.writeText(
                buildString {
                    appendLine("Bucket,Occurrences")
                    Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram()).forEach { (bucket, count) ->
                        appendLine("$bucket,$count")
                    }
                },
            )
        }
    }
}
