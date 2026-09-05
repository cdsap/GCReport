package io.github.cdsap.gcreport.plugin.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.GCReportExtension
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.GCCollectionMetrics
import io.github.cdsap.gcreport.plugin.model.GCEntry

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val gcEntries: List<GCEntry>,
    private val log: String,
    private val extension: GCReportExtension,
) {
    fun report() {
        val collectionMetrics = GCCollectionMetrics(gcEntries)
        develocityConfiguration.buildScan {
            collectionMetrics.countsByDescription().forEach { (description, count) ->
                value("gc-${log.getFileName()}-$description", "$count")
            }
            val counter = collectionMetrics.totalCollections()
            if (counter != 0) {
                value("gc-${log.getFileName()}-total-collections", "$counter")
            }
            if (extension.histogramEnabled.get()) {
                val histogram =
                    Histogram(extension.histogramBucket.get()).getHistogram(
                        collectionMetrics.entriesForHistogram(),
                    )
                var histogramText = "["
                histogram.forEach {
                    histogramText += "\"${it.first}\": \"${it.second}\", "
                }
                if (histogramText.isNotEmpty()) {
                    value("gc-${log.getFileName()}-histogram", "${histogramText.dropLast(1)}]")
                }
            }
        }
    }
}
