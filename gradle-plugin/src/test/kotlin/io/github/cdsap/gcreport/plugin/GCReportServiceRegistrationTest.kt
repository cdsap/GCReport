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
    fun `with Develocity enableConsoleLog true enables console report output`() {
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
                enableConsoleLog.set(true)
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

    @Test
    fun `apply reuses extension instance and looks up Develocity inside rootProject receiver`() {
        val plugin =
            File("src/main/kotlin/io/github/cdsap/gcreport/plugin/GCReportPlugin.kt").readText()

        assertTrue(
            plugin.contains("target.extensions.create<GCReportExtension>(\"gcReport\")"),
        )
        assertFalse(plugin.contains("getByName(\"gcReport\")"))
        assertFalse(plugin.contains("as GCReportExtension"))

        val applyBody =
            plugin
                .substringAfter("override fun apply(target: Project) {")
                .substringBeforeLast("}")
                .trim()
        val rootProjectBlockStart = applyBody.indexOf("target.gradle.rootProject {")
        assertTrue(rootProjectBlockStart >= 0, "expected rootProject receiver block")

        val beforeRootProject = applyBody.substring(0, rootProjectBlockStart)
        assertFalse(
            beforeRootProject.contains("DevelocityPresence.findExtension"),
            "Develocity lookup must not run eagerly before the rootProject block",
        )

        val rootProjectBlock = applyBody.substring(rootProjectBlockStart)
        assertTrue(
            rootProjectBlock.contains("DevelocityPresence.findExtension(this)"),
            "Develocity lookup should use the rootProject receiver",
        )
        assertFalse(
            rootProjectBlock.contains("DevelocityPresence.findExtension(target.gradle.rootProject)"),
            "unused rootProject receiver: lookup should not re-qualify through target.gradle.rootProject",
        )
    }
}
