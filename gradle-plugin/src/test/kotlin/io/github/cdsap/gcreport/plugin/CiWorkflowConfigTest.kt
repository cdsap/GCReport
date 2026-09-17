package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CiWorkflowConfigTest {
    @Test
    fun `ci workflow runs a single gradlew build covering the full lifecycle`() {
        val workflow = File("../.github/workflows/build.yaml").canonicalFile
        assertTrue(workflow.isFile, "CI workflow should exist at ${workflow.path}")

        val contents = workflow.readText()
        val gradleInvocations =
            Regex("""^\s*run:\s*\./gradlew\b.*$""", RegexOption.MULTILINE)
                .findAll(contents)
                .map { it.value.trim() }
                .toList()

        assertEquals(
            listOf("run: ./gradlew build"),
            gradleInvocations,
            "CI must invoke Gradle exactly once with ./gradlew build " +
                "(covers ktlintCheck, test, validatePlugins, and assemble)",
        )
        assertTrue(
            contents.contains("GE_API_KEY: \${{ secrets.GE_API }}"),
            "Build step must pass GE_API_KEY",
        )
        assertTrue(
            contents.contains("GE_URL: \${{ secrets.GE_URL }}"),
            "Build step must pass GE_URL",
        )
    }
}
