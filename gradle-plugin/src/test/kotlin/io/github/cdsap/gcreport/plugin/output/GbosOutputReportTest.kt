package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.GCEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GbosOutputReportTest {
    @TempDir
    lateinit var outputDirectory: File

    @Test
    fun `writes the same observations as JSON and NDJSON`() {
        val json = File(outputDirectory, "gbos.json")
        val ndjson = File(outputDirectory, "gbos.ndjson")
        val observations = observations()

        GbosOutputReport(json, ndjson).write(observations, writeJson = true, writeNdjson = true)

        val report = Json.parseToJsonElement(json.readText()).jsonObject
        assertEquals("1.0.0", report.getValue("schemaVersion").jsonPrimitive.content)
        assertEquals(2, report.getValue("observations").jsonArray.size)
        assertEquals(2, ndjson.readLines().size)
        assertTrue(ndjson.readLines().all { !it.contains('\n') })
        assertEquals(
            report.getValue("observations").jsonArray.map { it.jsonObject },
            ndjson.readLines().map { Json.parseToJsonElement(it).jsonObject },
        )
    }

    @Test
    fun `removes disabled sinks without leaving stale content`() {
        val json = File(outputDirectory, "gbos.json").apply { writeText("stale") }
        val ndjson = File(outputDirectory, "gbos.ndjson").apply { writeText("stale") }

        GbosOutputReport(json, ndjson).write(emptyList(), writeJson = false, writeNdjson = false)

        assertFalse(json.exists())
        assertFalse(ndjson.exists())
    }

    @Test
    fun `keeps the previous file when replacement fails`() {
        val json = File(outputDirectory, "gbos.json").apply { mkdirs() }
        val ndjson = File(outputDirectory, "gbos.ndjson")

        assertThrows(Exception::class.java) {
            GbosOutputReport(json, ndjson).write(observations(), writeJson = true, writeNdjson = false)
        }
        assertTrue(json.isDirectory)
    }

    private fun observations() =
        GbosDevelocityProjection.observations(
            "/tmp/gradle_gc.log",
            listOf(entry("Pause Young"), entry("Concurrent Mark Cycle")),
        )

    private fun entry(description: String) =
        GCEntry(
            timeStamp = "1.00",
            timeStampUnit = "s",
            id = description,
            description = description,
            duration = "10",
            durationUnit = "ms",
        )
}
