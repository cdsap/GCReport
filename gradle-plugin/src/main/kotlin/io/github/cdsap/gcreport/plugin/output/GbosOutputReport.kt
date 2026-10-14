package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gbos.core.GbosJson
import io.github.cdsap.gbos.core.GbosObservation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Writes the canonical GBOS observations to the format-specific local sinks. */
internal class GbosOutputReport(
    private val outputJson: File,
    private val outputNdjson: File,
) {
    fun write(
        observations: List<GbosObservation>,
        writeJson: Boolean,
        writeNdjson: Boolean,
    ) {
        writeSink(outputJson, writeJson && observations.isNotEmpty()) {
            GbosJson.encodeReport(observations)
        }
        writeSink(outputNdjson, writeNdjson && observations.isNotEmpty()) {
            observations.joinToString("\n", postfix = "\n", transform = ::encodeCompact)
        }
    }

    private fun encodeCompact(observation: GbosObservation): String =
        Json.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(GbosJson.encode(observation)))

    private fun writeSink(
        output: File,
        enabled: Boolean,
        content: () -> String,
    ) {
        if (!enabled) {
            output.delete()
            return
        }
        output.parentFile?.mkdirs()
        val temporary = File.createTempFile(".${output.name}.", ".tmp", output.parentFile)
        try {
            temporary.writeText(content())
            Files.move(
                temporary.toPath(),
                output.toPath(),
                ATOMIC_MOVE,
                REPLACE_EXISTING,
            )
        } finally {
            temporary.delete()
        }
    }
}
