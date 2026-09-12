package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CompatibilityMatrixTest {
    @Test
    fun `ci workflow exercises the documented java and gradle compatibility matrix`() {
        val workflow = File("../.github/workflows/build.yaml").canonicalFile
        assertTrue(workflow.isFile, "CI workflow should exist at ${workflow.path}")

        val workflowText = workflow.readText()
        assertTrue(
            workflowText.contains("fail-fast: false"),
            "Compatibility matrix should keep other cells running when one fails",
        )
        assertTrue(
            workflowText.contains("java: [ 17, 21 ]") ||
                workflowText.contains("java: [17, 21]"),
            "CI should test JDK 17 and 21 boundaries",
        )
        for (gradle in SUPPORTED_GRADLE_VERSIONS) {
            assertTrue(
                workflowText.contains("'$gradle'") || workflowText.contains("\"$gradle\""),
                "CI matrix should include Gradle $gradle",
            )
        }
        assertTrue(
            workflowText.contains("testGradleVersion=\${{ matrix.gradle }}") ||
                workflowText.contains("TEST_GRADLE_VERSION: \${{ matrix.gradle }}"),
            "CI must drive TestKit Gradle version from the matrix",
        )
        assertTrue(
            workflowText.contains("java-version: \${{ matrix.java }}"),
            "CI must install the matrix JDK",
        )
        assertTrue(
            workflowText.contains("junit-test-reports-jdk\${{ matrix.java }}-gradle\${{ matrix.gradle }}") ||
                workflowText.contains("name: junit-test-reports-\${{ matrix.java }}-\${{ matrix.gradle }}"),
            "Matrix builds need unique JUnit artifact names",
        )
    }

    @Test
    fun `readme documents supported gradle and java ranges`() {
        val readme = File("../README.md").canonicalFile
        assertTrue(readme.isFile, "README should exist at ${readme.path}")

        val readmeText = readme.readText()
        assertTrue(
            readmeText.contains("### Compatibility"),
            "README must include a Compatibility section",
        )
        assertTrue(
            Regex("""Gradle[:\s*]*\**${Regex.escape(SUPPORTED_GRADLE_VERSIONS.first())}\+\**""")
                .containsMatchIn(readmeText),
            "README must document the minimum supported Gradle version",
        )
        assertTrue(
            Regex("""Java[:\s*]*\**17\**\s+and\s+\**21\**""")
                .containsMatchIn(readmeText),
            "README must document the tested Java versions",
        )
    }

    @Test
    fun `gradle plugin build forwards testGradleVersion into TestKit tests`() {
        val buildGradle = File("build.gradle.kts").canonicalFile.readText()
        assertTrue(
            buildGradle.contains("testGradleVersion"),
            "build.gradle.kts must forward testGradleVersion to the test JVM for TestKit",
        )
    }

    companion object {
        val SUPPORTED_GRADLE_VERSIONS = listOf("8.9", "8.12.1", "8.14.3")
    }
}
