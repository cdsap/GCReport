package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class GCLogReaderTest {
    @Test
    fun `parse complex G1 log with multiple events`() {
        val logFile = File("src/test/resources/correct_g1_log")
        val reader = GCLogReader(logFile)
        val gcEntries = reader.parse()

        assertEquals(19, gcEntries.size)

        val expectedEntry1 =
            GCEntry(
                timeStamp = "1.164",
                timeStampUnit = "s",
                id = "0",
                description = "Pause Young (Concurrent Start) (Metadata GC Threshold)",
                duration = "4.981",
                durationUnit = "ms",
                memoryBefore = "194",
                memoryBeforeUnit = "M",
                memoryAfter = "24",
                memoryAfterUnit = "M",
                memory = "4096",
                memoryUnit = "M",
            )
        assertEquals(expectedEntry1, gcEntries[0])
    }
}
