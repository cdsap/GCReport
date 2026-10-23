package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

class GCLogReaderTest {
    @Test
    fun `load existing logs and ignore missing logs`() {
        val existingLog = File("src/test/resources/correct_g1_log")
        val missingLog = createTempFile(prefix = "missing-gc-log").toFile()
        assertTrue(missingLog.delete())

        val logs = GCLogLoader().load(listOf(missingLog.path, existingLog.path))

        assertEquals(1, logs.size)
        assertEquals(existingLog, logs.single().file)
        assertEquals(19, logs.single().entries.size)
    }

    @Test
    fun `load existing unparsable log with empty entries`() {
        val logFile = createTempFile(prefix = "unparsable-gc-log")
        try {
            logFile.writeText("not a GC log")

            val logs = GCLogLoader().load(listOf(logFile.toString()))

            assertEquals(1, logs.size)
            assertEquals(logFile.toFile(), logs.single().file)
            assertEquals(emptyList<GCEntry>(), logs.single().entries)
        } finally {
            logFile.toFile().delete()
        }
    }

    @Test
    fun `parse in-memory log lines`() {
        val gcEntries =
            GCLogParser().parse(
                listOf(
                    "[1.164s][info   ][gc          ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold) " +
                        "194M->24M(4096M) 4.981ms",
                ),
            )

        assertEquals(
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
            ),
            gcEntries.single(),
        )
    }

    @Test
    fun `parse concurrent mark cycle from in-memory log lines`() {
        val gcEntries =
            GCLogParser().parse(
                listOf(
                    "[1.000s][info   ][gc          ] GC(1) Pause Remark 25M->25M(4096M) 0.941ms",
                    "[1.001s][info   ][gc          ] GC(1) Pause Cleanup 26M->26M(4096M) 0.010ms",
                    "[1.002s][info   ][gc          ] GC(1) Concurrent Mark Cycle 10.974ms",
                ),
            )

        assertEquals(
            GCEntry(
                timeStamp = "1.002",
                timeStampUnit = "s",
                id = "1",
                description = "Concurrent Mark Cycle",
                duration = "10.974",
                durationUnit = "ms",
            ),
            gcEntries.single(),
        )
    }

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
