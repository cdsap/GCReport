package io.github.cdsap.gcreport.plugin.report

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.GCReportExtension
import io.github.cdsap.gcreport.plugin.output.DevelocityValues
import io.github.cdsap.gcreport.plugin.parser.GCLogReader
import java.io.File

class DevelocityReport(
    private val develocityConfiguration: DevelocityConfiguration,
    private val extension: GCReportExtension,
) {
    fun report() {
        develocityConfiguration.buildScan.buildFinished {
            val logs = extension.logs.get()
            val histogramEnabled = extension.histogramEnabled.get()
            val histogramBucket = extension.histogramBucket.get()
            logs.filter { File(it).exists() }.forEach {
                val gcEntries = GCLogReader(File(it)).parse()
                DevelocityValues(
                    develocityConfiguration,
                    gcEntries,
                    it,
                    histogramEnabled,
                    histogramBucket,
                ).report()
            }
        }
    }
}
