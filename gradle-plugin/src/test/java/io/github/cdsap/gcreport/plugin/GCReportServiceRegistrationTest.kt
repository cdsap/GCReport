package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GCReportServiceRegistrationTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `GCReportService has a single registration path that configures enabledReport`() {
        val mainKotlin = File("src/main/kotlin")
        require(mainKotlin.isDirectory) { "Expected Kotlin sources at ${mainKotlin.absolutePath}" }

        val kotlinSources =
            mainKotlin.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        assertTrue(kotlinSources.none { it.name == "ServiceHandler.kt" })
        assertTrue(kotlinSources.none { it.name == "ConsoleReport.kt" })

        val registrationSites =
            kotlinSources.filter { source ->
                val text = source.readText()
                text.contains("\"gcReportService\"") && text.contains("registerIfAbsent")
            }

        assertEquals(1, registrationSites.size, "expected exactly one GCReportService registration path")
        assertEquals("GCReportPlugin.kt", registrationSites.single().name)
        assertTrue(registrationSites.single().readText().contains("enabledReport"))
    }

    @Test
    fun `without Develocity console report remains enabled by default registration path`() {
        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        gradleProperties.writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog
            """.trimIndent(),
        )

        val buildFile = File(testProjectDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                id("io.github.cdsap.gcreport")
                java
            }

            gcReport {
                logs.set(listOf("$gcLog"))
                enableConsoleLog.set(false)
            }
            """,
        )

        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks")
                .withPluginClasspath()
                .build()

        assertTrue(result.output.contains("GC Log: gc.log"))
        assertTrue(result.output.contains("Collection type"))
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
    }
}
