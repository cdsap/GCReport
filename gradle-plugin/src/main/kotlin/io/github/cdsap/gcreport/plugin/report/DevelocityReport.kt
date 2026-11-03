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
            val gbosDevelocityEnabled = extension.gbosDevelocityEnabled.get()
            val histogramEnabled = extension.histogramEnabled.get()
            val histogramBucket = extension.histogramBucket.get()
            GCLogLoader().load(extension.logs.get()).forEach { log ->
                DevelocityValues(
                    develocityConfiguration,
                    log.entries,
                    log.file.path,
                    histogramEnabled,
                    histogramBucket,
                    legacyValuesEnabled = extension.legacyDevelocityValuesEnabled.get(),
                    gbosEnabled = gbosDevelocityEnabled,
                ).report()
            }
        }
    }
}
