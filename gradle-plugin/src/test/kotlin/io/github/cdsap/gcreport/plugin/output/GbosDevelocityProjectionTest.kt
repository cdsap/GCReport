package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gbos.core.GbosAttributeValue
import io.github.cdsap.gbos.core.GbosJson
import io.github.cdsap.gcreport.plugin.model.GCEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GbosDevelocityProjectionTest {
    @Test
    fun `groups GC events by action without changing existing entry counts`() {
        val observations =
            GbosDevelocityProjection.observations(
                "/tmp/gradle_gc.log",
                listOf(
                    entry("Pause Young (Normal)"),
                    entry("Pause Young (Normal)"),
                    entry("Concurrent Mark Cycle"),
                ),
            )

        assertEquals(2, observations.size)
        assertEquals(
            setOf("Concurrent Mark Cycle", "Pause Young (Normal)"),
            observations.map { it.textAttribute("jvm.gc.action") }.toSet(),
        )

        val normal =
            observations.single { observation ->
                observation.textAttribute("jvm.gc.action") == "Pause Young (Normal)"
            }
        assertEquals(2.0, normal.measurements.single().value)
        assertEquals("jvm.gc", normal.scope)
        assertEquals("gradle-daemon", normal.textAttribute("jvm.process.role"))
    }

    @Test
    fun `encodes a schema-shaped observation with the shared core`() {
        val observation =
            GbosDevelocityProjection.observations(
                "/tmp/kotlin_gc.log",
                listOf(entry("Pause Young")),
            ).single()
        val json = GbosJson.encode(observation).let { Json.parseToJsonElement(it).jsonObject }

        assertEquals("1.0.0", json.getValue("schemaVersion").jsonPrimitive.content)
        assertEquals("gc-report", json.getValue("producer").jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("kotlin-daemon", json.getValue("attributes").jsonObject.getValue("jvm.process.role").jsonPrimitive.content)
        assertEquals(1L, json.getValue("measurements").jsonArray.single().jsonObject.getValue("value").jsonPrimitive.long)
        assertTrue(json.containsKey("measurements"))
    }

    private fun entry(description: String) =
        GCEntry(
            timeStamp = "1.00",
            timeStampUnit = "s",
            id = "1",
            description = description,
            duration = "10",
            durationUnit = "ms",
        )

    private fun io.github.cdsap.gbos.core.GbosObservation.textAttribute(name: String): String =
        (attributes.getValue(name) as GbosAttributeValue.Text).value
}
