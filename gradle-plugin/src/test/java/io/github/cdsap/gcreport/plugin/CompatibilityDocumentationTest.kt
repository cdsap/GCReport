package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class CompatibilityDocumentationTest {
    @Test
    fun `readme documents gradle java and gc compatibility matching the build`() {
        val readme = File("../README.md").canonicalFile
        assertTrue(readme.isFile, "README should exist at ${readme.path}")
        val readmeText = readme.readText()

        val buildGradle = File("build.gradle.kts").canonicalFile.readText()
        val toolchainMatch = TOOLCHAIN_REGEX.find(buildGradle)
        assertTrue(
            toolchainMatch != null,
            "build.gradle.kts must pin kotlin jvmToolchain so README Java floor is real",
        )
        val toolchainJava = toolchainMatch!!.groupValues[1]

        assertTrue(
            readmeText.contains("### Compatibility"),
            "README must include a Compatibility section",
        )
        assertTrue(
            Regex("""Gradle[:\s*]*\**${Regex.escape(MINIMUM_GRADLE_VERSION)}\+\**""")
                .containsMatchIn(readmeText),
            "README must document minimum Gradle $MINIMUM_GRADLE_VERSION+",
        )
        assertTrue(
            Regex("""Java[:\s*]*\**${Regex.escape(toolchainJava)}\+?\**""")
                .containsMatchIn(readmeText),
            "README minimum Java must match jvmToolchain($toolchainJava)",
        )
        assertTrue(
            readmeText.contains("-Xlog:gc"),
            "README must state the supported unified JVM GC log format (-Xlog:gc*)",
        )
        assertTrue(
            readmeText.contains("G1") && readmeText.contains("Parallel"),
            "README must state supported GC collectors G1 and Parallel",
        )
    }

    @Test
    fun `readme plugins snippet uses the current plugin id and version`() {
        val readmeText = File("../README.md").canonicalFile.readText()
        val buildGradle = File("build.gradle.kts").canonicalFile.readText()

        val versionMatch = VERSION_REGEX.find(buildGradle)
        assertTrue(versionMatch != null, "build.gradle.kts must declare version")
        val version = versionMatch!!.groupValues[1]

        assertTrue(
            readmeText.contains("""id("io.github.cdsap.gcreport") version "$version""""),
            "README plugins snippet must use id io.github.cdsap.gcreport and version $version",
        )

        val pluginIdMatch = PLUGIN_ID_REGEX.find(buildGradle)
        assertEquals(
            "io.github.cdsap.gcreport",
            pluginIdMatch?.groupValues?.get(1),
            "Published plugin id must remain io.github.cdsap.gcreport",
        )
    }

    @Test
    fun `service handler still depends on the gradle 8_9 serviceOf package`() {
        val serviceHandler =
            File("src/main/kotlin/io/github/cdsap/gcreport/plugin/ServiceHandler.kt")
                .canonicalFile
                .readText()

        assertTrue(
            serviceHandler.contains("org.gradle.internal.extensions.core.serviceOf"),
            "Minimum Gradle $MINIMUM_GRADLE_VERSION+ in the README is tied to this serviceOf import path",
        )
    }

    private companion object {
        const val MINIMUM_GRADLE_VERSION = "8.9"
        val TOOLCHAIN_REGEX = Regex("""jvmToolchain\((\d+)\)""")
        val VERSION_REGEX = Regex("""(?m)^version\s*=\s*"([^"]+)"""")
        val PLUGIN_ID_REGEX = Regex("""(?m)^\s*id\s*=\s*"([^"]+)"""")
    }
}
