package io.github.cdsap.gcreport.plugin.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.GCReportExtension
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.GCEntry

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val gcEntries: List<GCEntry>,
    private val log: String,
    private val extension: GCReportExtension,
) {
    fun report() {
        develocityConfiguration.buildScan {
            gcEntries.groupBy { it.description }.forEach { t, u ->
                value("gc-${log.getFileName()}-$t", "${u.size}")
            }
            val counter = gcEntries.filter { it.description != "Concurrent Mark Cycle" }.count()
            if (counter != 0) {
                value("gc-${log.getFileName()}-total-collections", "$counter")
            }
            if (extension.histogramEnabled.get()) {
                val histogram =
                    Histogram(extension.histogramBucket.get()).getHistogram(
                        gcEntries.filter { it.description != "Concurrent Mark Cycle" },
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
