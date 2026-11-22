package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gbos.core.GbosJson
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry

internal object DevelocityCustomValueProjection {
    fun values(
        log: String,
        gcEntries: List<GCEntry>,
        histogramEnabled: Boolean,
        histogramBucket: Bucket,
        legacyValuesEnabled: Boolean,
        gbosEnabled: Boolean,
    ): List<Pair<String, String>> {
        return buildList {
            if (legacyValuesEnabled) {
                addAll(GcDevelocityProjection.values(log, gcEntries, histogramEnabled, histogramBucket))
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
