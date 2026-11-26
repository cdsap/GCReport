package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.FileNotFoundException
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

class GCLogReaderTest {
    @Test
    fun `parse missing file propagates read failure`() {
        val missingLog = createTempFile(prefix = "missing-gc-log").toFile()
        assertTrue(missingLog.delete())

        assertThrows<FileNotFoundException> { GCLogReader(missingLog).parse() }
    }

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
    fun `load existing logs in configured order`() {
        val firstLog = File("src/test/resources/correct_g1_log")
        val secondLog = createTempFile(prefix = "second-gc-log")
        try {
            secondLog.writeText(firstLog.readText())

            val logs = GCLogLoader().load(listOf(secondLog.toString(), firstLog.path))

            assertEquals(listOf(secondLog.toFile(), firstLog), logs.map { it.file })
        } finally {
            secondLog.toFile().delete()
        }
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
