package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal object GcDevelocityProjection {
    fun values(
        log: String,
        gcEntries: List<GCEntry>,
        histogramEnabled: Boolean,
        histogramBucket: Bucket,
    ): List<Pair<String, String>> {
        val metrics = GCReportMetrics.from(gcEntries)
        val logFileName = log.getFileName()

        return buildList {
            metrics.collectionTypeCounts().forEach { (description, count) ->
                add("gc-$logFileName-$description" to "$count")
            }
            val counter = metrics.totalCollections()
            if (counter != 0) {
                add("gc-$logFileName-total-collections" to "$counter")
            }
            if (histogramEnabled) {
                val histogram = Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram())
                add("gc-$logFileName-histogram" to HistogramValueFormatter.format(histogram))
            }
        }
    }
}
