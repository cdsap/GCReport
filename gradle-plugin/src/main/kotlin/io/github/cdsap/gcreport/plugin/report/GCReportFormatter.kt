package io.github.cdsap.gcreport.plugin.report

import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal class GCReportFormatter(
    private val histogramBucket: Bucket,
) {
    fun collectionCounts(metrics: GCReportMetrics): String =
        buildString {
            appendLine("Collection type,Occurrences")
            metrics.collectionTypeCounts().forEach { (description, count) ->
                appendLine("$description,$count")
            }
        }

    fun histogram(metrics: GCReportMetrics): String =
        buildString {
            appendLine("Bucket,Occurrences")
            Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram()).forEach { (bucket, count) ->
                appendLine("$bucket,$count")
            }
        }
}
