package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GCReportServiceRegistrationTest {
    @TempDir
    lateinit var testProjectDir: File

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
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeKotlinSettings(
            testProjectDir,
            gcLog,
            extraGcReportConfig = """enableConsoleLog.set(false)""",
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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
    fun `with Develocity enableConsoleLog true enables console report output`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)

        File(testProjectDir, "settings.gradle.kts").writeText(
            DevelocityTestSupport.settingsScript(
                configureBody =
                    """
                    buildScan {
                        publishing.onlyIf { false }
                    }
                    """.trimIndent(),
                includeGcReport = true,
                gcReportBody =
                    """
                    logs.set(listOf("$gcLog"))
                    enableConsoleLog.set(true)
                    """.trimIndent(),
            ),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)

        File(testProjectDir, "settings.gradle.kts").writeText(
            DevelocityTestSupport.settingsScript(
                configureBody =
                    """
                    buildScan {
                        publishing.onlyIf { false }
                    }
                    """.trimIndent(),
                includeGcReport = true,
                gcReportBody =
                    """
                    logs.set(listOf("$gcLog"))
                    enableConsoleLog.set(false)
                    """.trimIndent(),
            ),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeKotlinSettings(
            testProjectDir,
            gcLog,
            extraGcReportConfig =
                """
                histogramEnabled.set(true)
                histogramBucket.set(io.github.cdsap.gcreport.plugin.model.Bucket.SquareRoot)
                """.trimIndent(),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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

    @Test
    fun `settings plugin creates extension and looks up Develocity inside rootProject receiver`() {
        val plugin =
            File("src/main/kotlin/io/github/cdsap/gcreport/plugin/GCReportPlugin.kt").readText()
        val support =
            File("src/main/kotlin/io/github/cdsap/gcreport/plugin/GCReportPluginSupport.kt").readText()

        assertTrue(plugin.contains("Plugin<Settings>"))
        assertTrue(plugin.contains("GCReportPluginSupport.createExtension(target)"))
        assertFalse(plugin.contains("getByName(\"gcReport\")"))
        assertFalse(plugin.contains("as GCReportExtension"))

        assertTrue(support.contains("gradle.rootProject {"))
        assertTrue(support.contains("findDevelocity(rootProject)"))
        assertTrue(
            support.contains("sharedServices.registrations.findByName(SERVICE_NAME)"),
            "Service registration must be guarded once per build",
        )
    }

    @Test
    fun `project compatibility plugin still registers console report`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        File(testProjectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            """.trimIndent(),
        )
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.cdsap.gcreport.project")
                java
            }

            gcReport {
                logs.set(listOf("$gcLog"))
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

    private fun kotlinSources(): List<File> = File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
}
