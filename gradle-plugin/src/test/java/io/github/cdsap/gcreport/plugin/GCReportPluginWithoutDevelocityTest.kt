package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GCReportPluginWithoutDevelocityTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin generates Output with Gc report for G1`() {
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
        assertTrue(!result.output.contains("GC Histogram: gc.log"))
        assertTrue(!result.output.contains("Type: SquareRoot"))
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
    }

    @Test
    fun `plugin generates Output with Gc report for Parallel`() {
        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        gradleProperties.writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog -XX:+UseParallelGC
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
        assertTrue(!result.output.contains("GC Histogram: gc.log"))
        assertTrue(!result.output.contains("Type: SquareRoot"))
    }

    @Test
    fun `plugin generates Output with Gc report and Histogram`() {
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
        assertTrue(result.output.contains("Collection type"))
        assertTrue(result.output.contains("GC Histogram: gc.log"))
        assertTrue(result.output.contains("Type: SquareRoot"))
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
        assertTrue(testProjectDir.resolve("build/reports/gcreport/histogram_gc.csv").exists())
    }

    @Test
    fun `plugin doesn't generate output if file is incorrect`() {
        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/gc.log"
        val gcLogIncorrect = "${testProjectDir.absolutePath}/gc2.log"
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
                logs.set(listOf("$gcLogIncorrect"))
            }
            """,
        )

        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks")
                .withPluginClasspath()
                .build()

        assertTrue(!result.output.contains("GC Log: gc.log"))
        assertTrue(!result.output.contains("Collection type"))
        assertTrue(!result.output.contains("GC Histogram: gc.log"))
        assertTrue(!result.output.contains("Type: SquareRoot"))
    }

    @Test
    fun `plugin supports kotlin and gradle gc logs`() {
        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/gradle_gc.log"
        val gcKotlinLog = "${testProjectDir.absolutePath}/kotlin_gc.log"
        gradleProperties.writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog
            kotlin.daemon.jvmargs=-Xlog:gc*:file=$gcKotlinLog
            """.trimIndent(),
        )

        val buildFile = File(testProjectDir, "build.gradle.kts")

        buildFile.writeText(
            """

            plugins {
                id("io.github.cdsap.gcreport")
                kotlin("jvm") version "2.1.0"
            }

            repositories {
                mavenCentral()
            }

            gcReport {
                logs.set(listOf("$gcLog","$gcKotlinLog"))
                histogramEnabled.set(true)
            }
            """,
        )

        val kotlinClassContent =
            """
            package com.example

            class MyTestClass {
                fun hello() {
                    println("Hello from MyTestClass!")
                }
            }
            """.trimIndent()

        val kotlinFile = File(testProjectDir, "src/main/kotlin/com/example/MyTestClass.kt")
        kotlinFile.parentFile.mkdirs()
        kotlinFile.writeText(kotlinClassContent)

        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("assemble")
                .withPluginClasspath()
                .build()

        assertTrue(result.output.contains("GC Log: gradle_gc.log"))
        assertTrue(result.output.contains("GC Histogram: gradle_gc.log"))
        assertTrue(result.output.contains("GC Log: kotlin_gc.log"))
        assertTrue(result.output.contains("GC Histogram: kotlin_gc.log"))
        assertTrue(result.output.contains("Collection type"))
        assertTrue(result.output.contains("Type: FreedmanDiaconis"))
        assertTrue(testProjectDir.resolve("build/reports/gcreport/gradle_gc.csv").exists())
        assertTrue(testProjectDir.resolve("build/reports/gcreport/histogram_gradle_gc.csv").exists())
        assertTrue(testProjectDir.resolve("build/reports/gcreport/kotlin_gc.csv").exists())
        assertTrue(testProjectDir.resolve("build/reports/gcreport/histogram_kotlin_gc.csv").exists())
    }

    @Test
    fun `plugin compatible with configuration cache`() {
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
                 id("io.github.cdsap.gradleprocess") version "0.1.2"
                java
            }

            gcReport {
                logs.set(listOf("$gcLog"))
            }
            """,
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("clean", "assemble", "--configuration-cache")
            .withPluginClasspath()
            .build()

        val withConfigurationCache =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("clean", "assemble", "--configuration-cache")
                .withPluginClasspath()
                .build()
        assertTrue(withConfigurationCache.output.contains("Reusing configuration cache."))
    }
}
