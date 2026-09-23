package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gbos.core.GbosAttributeValue
import io.github.cdsap.gbos.core.GbosMeasurement
import io.github.cdsap.gbos.core.GbosObservation
import io.github.cdsap.gbos.core.GbosProducer
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.model.GCEntry

internal object GbosDevelocityProjection {
    const val OBSERVATION_CUSTOM_VALUE = "gbos.v1.producer.gc_report.observation"
    private const val SCHEMA_VERSION = "1.0.0"
    private const val PRODUCER_NAME = "gc-report"
    private const val PRODUCER_VERSION = "0.1.0"

    fun observations(
        log: String,
        entries: List<GCEntry>,
    ): List<GbosObservation> {
        val logFileName = log.getFileName()
        val role = if (logFileName.contains("kotlin", ignoreCase = true)) "kotlin-daemon" else "gradle-daemon"
        return entries
            .groupBy { it.description }
            .toSortedMap()
            .map { (action, actionEntries) ->
                GbosObservation(
                    schemaVersion = SCHEMA_VERSION,
                    producer =
                        GbosProducer(
                            PRODUCER_NAME,
                            PRODUCER_VERSION,
                        ),
                    scope = "jvm.gc",
                    aggregationScope = "entity",
                    attributes =
                        mapOf(
                            "log.file.name" to GbosAttributeValue.Text(logFileName),
                            "jvm.process.role" to GbosAttributeValue.Text(role),
                            "jvm.gc.action" to GbosAttributeValue.Text(action),
                        ),
                    measurements =
                        listOf(
                            GbosMeasurement(
                                name = "jvm.gc.events",
                                value = actionEntries.size.toDouble(),
                                unit = "{collection}",
                                aggregation = "count",
                            ),
                        ),
                )
            }
    }
}
