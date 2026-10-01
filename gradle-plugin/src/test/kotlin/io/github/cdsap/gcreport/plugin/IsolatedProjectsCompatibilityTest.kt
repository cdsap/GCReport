package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

/**
 * Backs the Plugin Portal `isolatedProjects` declarations in build.gradle.kts.
 * Isolated Projects violations fail the build, so compatible scenarios must succeed on both the
 * configuration cache store run and the reuse run.
 */
class IsolatedProjectsCompatibilityTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `settings plugin supports isolated projects in a multi-project build`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeKotlinSettings(
            testProjectDir,
            gcLog,
            extraSettings = """include("app", "lib")""",
        )
        writeMultiProjectBuilds()

        runTwiceWithIsolatedProjects()
    }

    @Test
    fun `settings plugin supports isolated projects with Develocity`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        // No publishing.onlyIf lambda: under TestKit it fails on configuration cache reuse even
        // without Isolated Projects, and no server is configured so no scan is published.
        File(testProjectDir, "settings.gradle.kts").writeText(
            DevelocityTestSupport.settingsScript(
                configureBody = "",
                includeGcReport = true,
                gcReportBody =
                    """
                    logs.set(listOf("$gcLog"))
                    enableConsoleLog.set(true)
                    """.trimIndent(),
            ) + "\ninclude(\"app\", \"lib\")",
        )
        writeMultiProjectBuilds()

        runTwiceWithIsolatedProjects()
    }

    @Test
    fun `project compatibility plugin applied to the root project works with isolated projects`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport()
        writeMultiProjectBuilds(rootBuild = projectPluginBuild(gcLog))

        runTwiceWithIsolatedProjects()
    }

    @Test
    fun `project compatibility plugin applied to a subproject is rejected by isolated projects`() {
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        writeSettingsWithoutGcReport()
        writeMultiProjectBuilds(appBuild = projectPluginBuild(gcLog))

        runTwiceAndAssertReuse("--configuration-cache")

        val isolated = runner(ISOLATED_PROJECTS_ARGUMENT).buildAndFail()
        assertIsolatedProjectsEnabled(isolated)
        assertTrue(
            isolated.output.contains("Project ':app' cannot access 'Project.extensions' functionality on another project ':'"),
            isolated.output,
        )
    }

    @Test
    fun `published plugin descriptors declare feature compatibility backed by these scenarios`() {
        assertEquals(
            mapOf(
                CONFIGURATION_CACHE_FEATURE to "DECLARED_SUPPORTED",
                ISOLATED_PROJECTS_FEATURE to "DECLARED_SUPPORTED",
            ),
            compatibilityFeatures("io.github.cdsap.gcreport"),
        )
        assertEquals(
            mapOf(
                CONFIGURATION_CACHE_FEATURE to "DECLARED_SUPPORTED",
                ISOLATED_PROJECTS_FEATURE to "DECLARED_UNSUPPORTED",
            ),
            compatibilityFeatures("io.github.cdsap.gcreport.project"),
        )
    }

    private fun compatibilityFeatures(pluginId: String): Map<String, String> {
        val descriptor =
            checkNotNull(javaClass.classLoader.getResource("META-INF/gradle-plugins/$pluginId.properties")) {
                "Missing plugin descriptor for $pluginId"
            }
        val properties = Properties().apply { descriptor.openStream().use { load(it) } }
        return properties
            .stringPropertyNames()
            .filter { it.startsWith("compatibility.feature.") }
            .associateWith { properties.getProperty(it) }
    }

    private fun runTwiceWithIsolatedProjects() {
        runTwiceAndAssertReuse(ISOLATED_PROJECTS_ARGUMENT, ::assertIsolatedProjectsEnabled)
    }

    private fun runTwiceAndAssertReuse(
        argument: String,
        extraAssertions: (BuildResult) -> Unit = {},
    ) {
        val store = runner(argument).build()
        assertTrue(store.output.contains("Configuration cache entry stored"), store.output)
        assertFalse(CONFIGURATION_CACHE_PROBLEMS.containsMatchIn(store.output), store.output)
        assertReport(store)
        extraAssertions(store)

        testProjectDir.resolve("build/reports/gcreport").deleteRecursively()

        val reuse = runner(argument).build()
        assertTrue(reuse.output.contains("Reusing configuration cache."), reuse.output)
        assertReport(reuse)
        extraAssertions(reuse)
    }

    private fun runner(argument: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("assemble", argument)
            .withPluginClasspath()

    private fun assertIsolatedProjectsEnabled(result: BuildResult) {
        assertTrue(result.output.contains("Isolated projects is an incubating feature.", ignoreCase = true), result.output)
    }

    private fun assertReport(result: BuildResult) {
        assertTrue(result.output.contains("Collection type"), result.output)
        assertEquals(1, result.output.split("GC Log: gc.log").size - 1, result.output)
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
        assertFalse(testProjectDir.resolve("app/build/reports/gcreport/gc.csv").exists())
        assertFalse(testProjectDir.resolve("lib/build/reports/gcreport/gc.csv").exists())
    }

    private fun writeSettingsWithoutGcReport() {
        File(testProjectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            include("app", "lib")
            """.trimIndent(),
        )
    }

    private fun writeMultiProjectBuilds(
        rootBuild: String = JAVA_BUILD,
        appBuild: String = JAVA_BUILD,
    ) {
        GCReportTestFixtures.writeMinimalBuild(testProjectDir, rootBuild)
        File(testProjectDir, "app").mkdirs()
        GCReportTestFixtures.writeMinimalBuild(File(testProjectDir, "app"), appBuild)
        File(testProjectDir, "lib").mkdirs()
        GCReportTestFixtures.writeMinimalBuild(File(testProjectDir, "lib"), JAVA_BUILD)
    }

    private fun projectPluginBuild(gcLog: String): String =
        """
        plugins {
            id("io.github.cdsap.gcreport.project")
            java
        }

        gcReport {
            logs.set(listOf("$gcLog"))
        }
        """.trimIndent()

    private companion object {
        const val ISOLATED_PROJECTS_ARGUMENT = "-Dorg.gradle.unsafe.isolated-projects=true"
        const val CONFIGURATION_CACHE_FEATURE = "compatibility.feature.configuration-cache"
        const val ISOLATED_PROJECTS_FEATURE = "compatibility.feature.isolated-projects"
        val CONFIGURATION_CACHE_PROBLEMS = Regex("""problems? (was|were) found""")
        val JAVA_BUILD =
            """
            plugins {
                java
            }
            """.trimIndent()
    }
}
