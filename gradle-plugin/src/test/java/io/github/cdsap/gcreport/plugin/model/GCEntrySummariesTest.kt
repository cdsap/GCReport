package io.github.cdsap.gcreport.plugin.model

import io.github.cdsap.gcreport.plugin.parser.GCLogReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class GCEntrySummariesTest {
    @Test
    fun `collectionTypeCounts groups all entries including Concurrent Mark Cycle`() {
        val entries =
            listOf(
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = "Pause Young (Normal)"),
                gcEntry(description = CONCURRENT_MARK_CYCLE),
                gcEntry(description = "Pause Full (System.gc())"),
            )

        assertEquals(
            mapOf(
                "Pause Young (Normal)" to 2,
                CONCURRENT_MARK_CYCLE to 1,
                "Pause Full (System.gc())" to 1,
            ),
            entries.collectionTypeCounts(),
        )
    }

    @Test
    fun `entriesForHistogram excludes Concurrent Mark Cycle`() {
        val concurrentMarkCycle = gcEntry(description = CONCURRENT_MARK_CYCLE)
        val youngPause = gcEntry(description = "Pause Young (Normal)")
        val entries = listOf(youngPause, concurrentMarkCycle, youngPause)

        assertEquals(listOf(youngPause, youngPause), entries.entriesForHistogram())
    }

    @Test
    fun `correct g1 log collection counts and histogram entries`() {
        val logFile = File("src/test/resources/correct_g1_log")
        val entries = GCLogReader(logFile).parse()

        assertEquals(19, entries.size)
        assertEquals(6, entries.collectionTypeCounts()[CONCURRENT_MARK_CYCLE])
        assertEquals(13, entries.entriesForHistogram().size)
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
