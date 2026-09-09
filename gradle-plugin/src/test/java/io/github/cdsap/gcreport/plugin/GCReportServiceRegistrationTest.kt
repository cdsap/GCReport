package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GCReportServiceRegistrationTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `GCReportService has a single registration path delegated to ServiceHandler`() {
        val mainKotlin = File("src/main/kotlin")
        require(mainKotlin.isDirectory) { "Expected Kotlin sources at ${mainKotlin.absolutePath}" }

        val kotlinSources =
            mainKotlin.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        assertTrue(kotlinSources.any { it.name == "ServiceHandler.kt" })
        assertTrue(kotlinSources.none { it.name == "ConsoleReport.kt" })

        val registrationSites =
            kotlinSources.filter { source ->
                val text = source.readText()
                text.contains("\"gcReportService\"") && text.contains("registerIfAbsent")
            }

        assertEquals(1, registrationSites.size, "expected exactly one GCReportService registration path")
        assertEquals("ServiceHandler.kt", registrationSites.single().name)

        val serviceHandler = registrationSites.single().readText()
        assertTrue(serviceHandler.contains("enabledReport"))
        assertTrue(serviceHandler.contains("histogramEnabled"))
        assertTrue(serviceHandler.contains("histogramBucket"))
        assertTrue(serviceHandler.contains("buildOutput"))
        assertTrue(serviceHandler.contains("parameters.logs"))

        val plugin = kotlinSources.single { it.name == "GCReportPlugin.kt" }.readText()
        assertTrue(plugin.contains("ServiceHandler"))
        assertFalse(plugin.contains("registerIfAbsent"))
        assertFalse(plugin.contains("\"gcReportService\""))
    }

    @Test
    fun `main sources do not use println`() {
        val mainSources = File("src/main")
        require(mainSources.isDirectory) { "Expected sources at ${mainSources.absolutePath}" }

        val offenders =
            mainSources
                .walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "java") }
                .filter { it.readText().contains("println(") }
                .map { it.relativeTo(mainSources).path }
                .toList()

        assertTrue(offenders.isEmpty(), "println found in src/main: $offenders")
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

    @Test
    fun `with Develocity enableConsoleLog false disables console report output`() {
        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        gradleProperties.writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog
            """.trimIndent(),
        )

        val settingsGradle = File(testProjectDir, "settings.gradle.kts")
        settingsGradle.writeText(
            """
            plugins {
                id("com.gradle.develocity") version "3.19"
            }
            develocity {
                buildScan {
                    publishing.onlyIf { false }
                }
            }
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

        assertFalse(result.output.contains("GC Log: gc.log"))
        assertFalse(result.output.contains("Collection type"))
        assertFalse(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
    }
}
