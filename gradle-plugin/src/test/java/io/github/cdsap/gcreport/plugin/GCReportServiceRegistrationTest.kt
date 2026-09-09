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
}
