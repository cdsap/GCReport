package io.github.cdsap.gcreport.plugin.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GCCollectionMetricsTest {
    @Test
    fun `countsByDescription groups all entries including Concurrent Mark Cycle`() {
        val entries =
            listOf(
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = GCCollectionMetrics.CONCURRENT_MARK_CYCLE),
                gcEntry(description = "Pause Full (System.gc())"),
            )
        val metrics = GCCollectionMetrics(entries)

        assertEquals(
            mapOf(
                "Pause Young (Normal)" to 2,
                GCCollectionMetrics.CONCURRENT_MARK_CYCLE to 1,
                "Pause Full (System.gc())" to 1,
            ),
            metrics.countsByDescription(),
        )
    }

    @Test
    fun `entriesForHistogram excludes Concurrent Mark Cycle`() {
        val concurrentMarkCycle = gcEntry(description = GCCollectionMetrics.CONCURRENT_MARK_CYCLE)
        val youngPause = gcEntry(description = "Pause Young (Normal)")
        val entries = listOf(youngPause, concurrentMarkCycle, youngPause)
        val metrics = GCCollectionMetrics(entries)

        assertEquals(listOf(youngPause, youngPause), metrics.entriesForHistogram())
    }

    @Test
    fun `totalCollections counts entries excluding Concurrent Mark Cycle`() {
        val entries =
            listOf(
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = GCCollectionMetrics.CONCURRENT_MARK_CYCLE),
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = GCCollectionMetrics.CONCURRENT_MARK_CYCLE),
            )
        val metrics = GCCollectionMetrics(entries)

        assertEquals(2, metrics.totalCollections())
    }

    private fun gcEntry(description: String): GCEntry =
        GCEntry(
            timeStamp = "1.00",
            timeStampUnit = "s",
            id = "1",
            description = description,
            duration = "10",
            durationUnit = "ms",
        )
}
