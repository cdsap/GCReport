package io.github.cdsap.gcreport.plugin.output

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry

class DevelocityValues(
    private val develocityConfiguration: DevelocityConfiguration,
    private val gcEntries: List<GCEntry>,
    private val log: String,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
    private val legacyValuesEnabled: Boolean,
    private val gbosEnabled: Boolean,
) {
    fun report() {
        val values =
            DevelocityValueProjection.values(
                log,
                gcEntries,
                histogramEnabled,
                histogramBucket,
                legacyValuesEnabled,
                gbosEnabled,
            )
        develocityConfiguration.buildScan { buildScan ->
            values.forEach { (name, value) ->
                buildScan.value(name, value)
            }
        }
    }
}
