package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
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
        assertTrue(
            readmeText.contains("settings.gradle"),
            "README must document settings.gradle application",
        )
        assertTrue(
            readmeText.contains("io.github.cdsap.gcreport.project"),
            "README must document the project compatibility plugin id",
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

        val pluginIds =
            PLUGIN_ID_REGEX
                .findAll(buildGradle)
                .map { it.groupValues[1] }
                .toList()
        assertTrue(
            pluginIds.contains("io.github.cdsap.gcreport"),
            "Published plugin id must remain io.github.cdsap.gcreport",
        )
        assertTrue(
            pluginIds.contains("io.github.cdsap.gcreport.project"),
            "Published compatibility plugin id must be io.github.cdsap.gcreport.project",
        )
    }

    @Test
    fun `plugin injects BuildEventsListenerRegistry matching documented gradle floor`() {
        val plugin =
            File("src/main/kotlin/io/github/cdsap/gcreport/plugin/GCReportPlugin.kt")
                .canonicalFile
                .readText()

        assertTrue(
            plugin.contains("BuildEventsListenerRegistry"),
            "Minimum Gradle $MINIMUM_GRADLE_VERSION+ in the README is tied to BuildEventsListenerRegistry injection",
        )
        assertTrue(
            plugin.contains("@Inject"),
            "GCReportPlugin must obtain BuildEventsListenerRegistry via @Inject",
        )
        assertTrue(
            plugin.contains("Plugin<Settings>"),
            "GCReportPlugin must be a settings plugin",
        )
        assertFalse(
            plugin.contains("org.gradle.internal.extensions.core.serviceOf"),
            "GCReportPlugin must not depend on the internal serviceOf helper",
        )
    }

    private companion object {
        const val MINIMUM_GRADLE_VERSION = "8.9"
        val TOOLCHAIN_REGEX = Regex("""jvmToolchain\((\d+)\)""")
        val VERSION_REGEX = Regex("""(?m)^version\s*=\s*"([^"]+)"""")
        val PLUGIN_ID_REGEX = Regex("""(?m)^\s*id\s*=\s*"([^"]+)"""")
    }
}
