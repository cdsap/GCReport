package io.github.cdsap.gcreport.plugin.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.GCReportExtension
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val gcEntries: List<GCEntry>,
    private val log: String,
    private val extension: GCReportExtension,
) {
    fun report() {
        val metrics = GCReportMetrics.from(gcEntries)
        develocityConfiguration.buildScan { buildScan ->
            metrics.collectionTypeCounts().forEach { (description, count) ->
                buildScan.value("gc-${log.getFileName()}-$description", "$count")
            }
            val counter = metrics.totalCollections()
            if (counter != 0) {
                buildScan.value("gc-${log.getFileName()}-total-collections", "$counter")
            }
            if (extension.histogramEnabled.get()) {
                val histogram =
                    Histogram(extension.histogramBucket.get()).getHistogram(
                        metrics.entriesForHistogram(),
                    )
                var histogramText = "["
                histogram.forEach {
                    histogramText += "\"${it.first}\": \"${it.second}\", "
                }
                if (histogramText.isNotEmpty()) {
                    buildScan.value("gc-${log.getFileName()}-histogram", "${histogramText.dropLast(1)}]")
                }
            }
        }
    }
}
