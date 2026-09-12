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
        assertTrue(serviceHandler.contains("parameters.logs.set("))
        assertTrue(serviceHandler.contains("parameters.enabledReport.convention(true)"))
        assertFalse(serviceHandler.contains("project.provider"))

        val plugin = kotlinSources.single { it.name == "GCReportPlugin.kt" }.readText()
        assertTrue(plugin.contains("ServiceHandler"))
        assertFalse(plugin.contains("registerIfAbsent"))
        assertFalse(plugin.contains("\"gcReportService\""))
        assertTrue(plugin.contains("histogramEnabled.convention(false)"))
        assertTrue(plugin.contains("histogramBucket.convention("))
        assertTrue(plugin.contains("enableConsoleLog.convention(false)"))

        val service = kotlinSources.single { it.name == "GCReportService.kt" }.readText()
        assertTrue(service.contains("val logs: ListProperty<String>"))
        assertTrue(service.contains("val histogramEnabled: Property<Boolean>"))
        assertTrue(service.contains("val histogramBucket: Property<Bucket>"))
        assertTrue(service.contains("val buildOutput: DirectoryProperty"))
        assertTrue(service.contains("val enabledReport: Property<Boolean>"))
        assertFalse(service.contains("var logs:"))
        assertFalse(service.contains("Provider<List<String>>"))

        val extension = kotlinSources.single { it.name == "GCReportExtension.kt" }.readText()
        assertTrue(extension.contains("abstract class GCReportExtension"))
        assertTrue(extension.contains("abstract val logs: ListProperty<String>"))
        assertTrue(extension.contains("abstract val histogramEnabled: Property<Boolean>"))
        assertTrue(extension.contains("abstract val histogramBucket: Property<Bucket>"))
        assertTrue(extension.contains("abstract val enableConsoleLog: Property<Boolean>"))
        assertFalse(extension.contains("ObjectFactory"))
        assertFalse(extension.contains("open class GCReportExtension"))
    }

    @Test
    fun `Develocity providers are resolved only inside buildFinished`() {
        val values = kotlinSources().single { it.name == "DevelocityValues.kt" }.readText()
        val report = kotlinSources().single { it.name == "DevelocityReport.kt" }.readText()

        assertFalse(values.contains("GCReportExtension"))
        assertFalse(values.contains(".get()"))
        assertTrue(values.contains("histogramEnabled: Boolean"))
        assertTrue(values.contains("histogramBucket: Bucket"))

        val buildFinishedIndex = report.indexOf("develocityConfiguration.buildScan.buildFinished")
        assertTrue(buildFinishedIndex >= 0)
        assertTrue(buildFinishedIndex < report.indexOf("extension.logs.get()"))
        assertTrue(buildFinishedIndex < report.indexOf("extension.histogramEnabled.get()"))
        assertTrue(buildFinishedIndex < report.indexOf("extension.histogramBucket.get()"))
        assertFalse(report.substring(0, buildFinishedIndex).contains(".get()"))
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

    @Test
    fun `managed extension conventions remain overridable by consumers`() {
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
                histogramEnabled.set(true)
                histogramBucket.set(io.github.cdsap.gcreport.plugin.model.Bucket.SquareRoot)
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
        assertTrue(result.output.contains("GC Histogram: gc.log"))
        assertTrue(result.output.contains("Type: SquareRoot"))
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
        assertTrue(testProjectDir.resolve("build/reports/gcreport/histogram_gc.csv").exists())
    }

    private fun kotlinSources(): List<File> = File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
}
