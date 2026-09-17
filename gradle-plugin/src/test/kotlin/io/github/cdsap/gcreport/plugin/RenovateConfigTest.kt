package io.github.cdsap.gcreport.plugin

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RenovateConfigTest {
    @Test
    fun `renovate config exists and uses recommended preset`() {
        val renovateConfig = File("../.github/renovate.json").canonicalFile

        assertTrue(renovateConfig.isFile, "Renovate config should exist at ${renovateConfig.path}")

        val json = JsonParser.parseString(renovateConfig.readText()).asJsonObject
        assertEquals(
            "https://docs.renovatebot.com/renovate-schema.json",
            json.get("\$schema").asString,
        )

        val extends = json.getAsJsonArray("extends")
        assertTrue(
            extends.any { it.asString == "config:recommended" },
            "Renovate config should extend config:recommended",
        )
    }
}
