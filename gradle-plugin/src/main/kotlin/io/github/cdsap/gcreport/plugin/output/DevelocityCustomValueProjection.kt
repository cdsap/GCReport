package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gbos.core.GbosJson
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal object DevelocityCustomValueProjection {
    fun values(
        log: String,
        gcEntries: List<GCEntry>,
        histogramEnabled: Boolean,
        histogramBucket: Bucket,
        legacyValuesEnabled: Boolean,
        gbosEnabled: Boolean,
    ): List<Pair<String, String>> {
        val metrics = GCReportMetrics.from(gcEntries)
        val logFileName = log.getFileName()

        return buildList {
            if (legacyValuesEnabled) {
                metrics.collectionTypeCounts().forEach { (description, count) ->
                    add("gc-$logFileName-$description" to "$count")
                }
                val counter = metrics.totalCollections()
                if (counter != 0) {
                    add("gc-$logFileName-total-collections" to "$counter")
                }
            }
            if (legacyValuesEnabled && histogramEnabled) {
                val histogram = Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram())
                var histogramText = "["
                histogram.forEach {
                    histogramText += "\"${it.first}\": \"${it.second}\", "
                }
                if (histogramText.isNotEmpty()) {
                    add("gc-$logFileName-histogram" to "${histogramText.dropLast(1)}]")
                }
            }
            if (gbosEnabled) {
                GbosDevelocityProjection.observations(log, gcEntries).forEach { observation ->
                    add(
                        GbosDevelocityProjection.OBSERVATION_CUSTOM_VALUE to GbosJson.encode(observation),
                    )
                }
            }
        }
    }
}
