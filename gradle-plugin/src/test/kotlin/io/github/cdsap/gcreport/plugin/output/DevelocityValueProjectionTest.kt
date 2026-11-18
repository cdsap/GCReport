package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gbos.core.GbosJson
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DevelocityValueProjectionTest {
    @Test
    fun `projects collection counts total and histogram in publication order`() {
        val values =
            DevelocityValueProjection.values(
                log = "/tmp/gradle_gc.log",
                gcEntries =
                    listOf(
                        entry("Pause Young", "1.00"),
                        entry("Concurrent Mark Cycle", "1.00"),
                        entry("Pause Full", "1.00"),
                    ),
                histogramEnabled = true,
                histogramBucket = Bucket.SquareRoot,
                legacyValuesEnabled = true,
                gbosEnabled = false,
            )

        assertEquals(
            listOf(
                "gc-gradle_gc.log-Pause Young" to "1",
                "gc-gradle_gc.log-Concurrent Mark Cycle" to "1",
                "gc-gradle_gc.log-Pause Full" to "1",
                "gc-gradle_gc.log-total-collections" to "2",
                "gc-gradle_gc.log-histogram" to "[\"1.00-End\": \"2\",]",
            ),
            values,
        )
    }

    @Test
    fun `omits zero total collections while retaining collection counts`() {
        val values =
            DevelocityValueProjection.values(
                log = "gc.log",
                gcEntries = listOf(entry("Concurrent Mark Cycle", "1.00")),
                histogramEnabled = false,
                histogramBucket = Bucket.Sturges,
                legacyValuesEnabled = true,
                gbosEnabled = false,
            )

        assertEquals(
            listOf("gc-gc.log-Concurrent Mark Cycle" to "1"),
            values,
        )
    }

    @Test
    fun `includes encoded GBOS values only when enabled`() {
        val entries = listOf(entry("Pause Young", "1.00"))
        val disabled =
            DevelocityValueProjection.values(
                log = "/tmp/gradle_gc.log",
                gcEntries = entries,
                histogramEnabled = false,
                histogramBucket = Bucket.Sturges,
                legacyValuesEnabled = false,
                gbosEnabled = false,
            )
        val enabled =
            DevelocityValueProjection.values(
                log = "/tmp/gradle_gc.log",
                gcEntries = entries,
                histogramEnabled = false,
                histogramBucket = Bucket.Sturges,
                legacyValuesEnabled = false,
                gbosEnabled = true,
            )

        assertEquals(emptyList<Pair<String, String>>(), disabled)
        assertEquals(
            listOf(
                GbosDevelocityProjection.OBSERVATION_CUSTOM_VALUE to
                    GbosJson.encode(GbosDevelocityProjection.observations("/tmp/gradle_gc.log", entries).single()),
            ),
            enabled,
        )
    }

    private fun entry(
        description: String,
        timeStamp: String,
    ) = GCEntry(
        timeStamp = timeStamp,
        timeStampUnit = "s",
        id = "1",
        description = description,
        duration = "10",
        durationUnit = "ms",
    )
}
