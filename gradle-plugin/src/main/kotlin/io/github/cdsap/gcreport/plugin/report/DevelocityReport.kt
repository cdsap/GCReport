package io.github.cdsap.gcreport.plugin.report

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.GCReportExtension
import io.github.cdsap.gcreport.plugin.output.DevelocityValues
import io.github.cdsap.gcreport.plugin.parser.GCLogLoader

class DevelocityReport(
    private val develocityConfiguration: DevelocityConfiguration,
    private val extension: GCReportExtension,
) {
    fun report() {
        develocityConfiguration.buildScan.buildFinished {
            val gbosEnabled = extension.gbosEnabled.get()
            val histogramEnabled = extension.histogramEnabled.get()
            val histogramBucket = extension.histogramBucket.get()
            GCLogLoader().load(extension.logs.get()).forEach { log ->
                // GBOS opt-in replaces legacy gc-* values; without it, legacy (and histogram) only.
                DevelocityValues(
                    develocityConfiguration,
                    log.entries,
                    log.file.path,
                    histogramEnabled,
                    histogramBucket,
                    legacyValuesEnabled = !gbosEnabled,
                    gbosEnabled = gbosEnabled,
                ).report()
            }
        }
    }
}
