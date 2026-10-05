package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Released `io.github.cdsap.gcreport` was a project plugin; it must keep working from build scripts
 * alongside settings application and the `io.github.cdsap.gcreport.project` alias.
 */
class LegacyProjectApplicationTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `legacy id applied in root build script writes report without Develocity`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport()
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, projectBuild(gcLog, LEGACY_ID))

        assertSingleReport(run())
    }

    @Test
    fun `legacy id applied in groovy root build script writes report`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport()
        File(testProjectDir, "build.gradle").writeText(
            """
            plugins {
                id 'io.github.cdsap.gcreport'
                id 'java'
            }

            gcReport {
                logs = ['$gcLog']
            }
            """.trimIndent(),
        )

        assertSingleReport(run())
    }

    @Test
    fun `legacy id applied in settings behaves as before`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeKotlinSettings(testProjectDir, gcLog)
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

        assertSingleReport(run())
    }

    @Test
    fun `legacy id in settings plus project alias in root build script configures once`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeKotlinSettings(testProjectDir, gcLog)
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, projectBuild(gcLog, PROJECT_ALIAS_ID))

        assertSingleReport(run())
    }

    @Test
    fun `legacy id in settings plus legacy id in root build script configures once`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeKotlinSettings(testProjectDir, gcLog)
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, projectBuild(gcLog, LEGACY_ID))

        assertSingleReport(run())
    }

    @Test
    fun `legacy id and project alias applied to the same project configure once`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport()
        GCReportTestFixtures.writeMinimalBuild(
            testProjectDir,
            projectBuild(gcLog, LEGACY_ID, PROJECT_ALIAS_ID),
        )

        assertSingleReport(run())
    }

    @Test
    fun `legacy id applied in root and subproject build scripts writes one root report`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport(extraSettings = """include("app")""")
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, projectBuild(gcLog, LEGACY_ID))
        File(testProjectDir, "app").mkdirs()
        GCReportTestFixtures.writeMinimalBuild(File(testProjectDir, "app"), projectBuild(gcLog, LEGACY_ID))

        assertSingleReport(run())
        assertFalse(testProjectDir.resolve("app/build/reports/gcreport/gc.csv").exists())
    }

    @Test
    fun `with Develocity legacy id in settings plus project alias registers once`() {
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
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, projectBuild(gcLog, PROJECT_ALIAS_ID))

        assertSingleReport(run())
    }

    @Test
    fun `legacy id applied in root build script is configuration cache compatible`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport()
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, projectBuild(gcLog, LEGACY_ID))

        val store = run("--configuration-cache")
        assertTrue(store.output.contains("Configuration cache entry stored"), store.output)
        assertSingleReport(store)

        testProjectDir.resolve("build/reports/gcreport").deleteRecursively()

        val reuse = run("--configuration-cache")
        assertTrue(reuse.output.contains("Reusing configuration cache."), reuse.output)
        assertSingleReport(reuse)
    }

    private fun run(vararg extraArguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", *extraArguments)
            .withPluginClasspath()
            .build()

    private fun assertSingleReport(result: BuildResult) {
        assertTrue(result.output.contains("Collection type"), result.output)
        assertEquals(1, result.output.split("GC Log: gc.log").size - 1, result.output)
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
    }

    private fun writeSettingsWithoutGcReport(extraSettings: String = "") {
        File(testProjectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            $extraSettings
            """.trimIndent(),
        )
    }

    private fun projectBuild(
        gcLog: String,
        vararg pluginIds: String,
    ): String =
        """
        plugins {
        ${pluginIds.joinToString("\n") { "    id(\"$it\")" }}
            java
        }

        gcReport {
            logs.set(listOf("$gcLog"))
        }
        """.trimIndent()

    private companion object {
        const val LEGACY_ID = "io.github.cdsap.gcreport"
        const val PROJECT_ALIAS_ID = "io.github.cdsap.gcreport.project"
    }
}
