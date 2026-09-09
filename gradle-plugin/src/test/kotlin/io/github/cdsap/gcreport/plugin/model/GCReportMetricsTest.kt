package io.github.cdsap.gcreport.plugin.model

import io.github.cdsap.gcreport.plugin.parser.GCLogReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class GCReportMetricsTest {
    @Test
    fun `collectionTypeCounts groups all entries including Concurrent Mark Cycle`() {
        val entries =
            listOf(
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = GCReportMetrics.CONCURRENT_MARK_CYCLE),
                gcEntry(description = "Pause Full (System.gc())"),
            )
        val metrics = GCReportMetrics.from(entries)

        assertEquals(
            mapOf(
                "Pause Young (Normal)" to 2,
                GCReportMetrics.CONCURRENT_MARK_CYCLE to 1,
                "Pause Full (System.gc())" to 1,
            ),
            metrics.collectionTypeCounts(),
        )
    }

    @Test
    fun `entriesForHistogram excludes Concurrent Mark Cycle`() {
        val concurrentMarkCycle = gcEntry(description = GCReportMetrics.CONCURRENT_MARK_CYCLE)
        val youngPause = gcEntry(description = "Pause Young (Normal)")
        val entries = listOf(youngPause, concurrentMarkCycle, youngPause)
        val metrics = GCReportMetrics.from(entries)

        assertEquals(listOf(youngPause, youngPause), metrics.entriesForHistogram())
    }

    @Test
    fun `totalCollections counts entries excluding Concurrent Mark Cycle`() {
        val entries =
            listOf(
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = GCReportMetrics.CONCURRENT_MARK_CYCLE),
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = GCReportMetrics.CONCURRENT_MARK_CYCLE),
            )
        val metrics = GCReportMetrics.from(entries)

        assertEquals(2, metrics.totalCollections())
    }

    @Test
    fun `correct g1 log collection counts and histogram entries`() {
        val logFile = File("src/test/resources/correct_g1_log")
        val metrics = GCReportMetrics.from(GCLogReader(logFile).parse())

        assertEquals(19, metrics.collectionTypeCounts().values.sum())
        assertEquals(6, metrics.collectionTypeCounts()[GCReportMetrics.CONCURRENT_MARK_CYCLE])
        assertEquals(13, metrics.entriesForHistogram().size)
        assertEquals(13, metrics.totalCollections())
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
