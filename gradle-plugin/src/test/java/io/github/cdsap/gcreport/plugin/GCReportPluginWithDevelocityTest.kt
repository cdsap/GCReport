package io.github.cdsap.gcreport.plugin

import com.google.gson.Gson
import io.github.cdsap.gcreport.plugin.model.Response
import io.github.cdsap.gcreport.plugin.model.Value
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.gradle.internal.impldep.org.junit.Assume
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.random.Random

class GCReportPluginWithDevelocityTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin generates Output with Gc report for G1`() {
        Assume.assumeTrue(
            "Develocity URL and Access Key are set",
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null,
        )
        val develocityUrl = System.getenv("GE_URL")
        val develocityAccessKey = System.getenv("GE_API_KEY")

        val gcFile = "gc.log"
        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/$gcFile"
        gradleProperties.writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog
            """.trimIndent(),
        )
        val settingsGradle = File(testProjectDir, "settings.gradle.kts")
        val randomValue = Random.nextInt(Int.MAX_VALUE).toString()

        settingsGradle.writeText(
            """
            plugins {
                id("com.gradle.develocity") version "3.19"
            }
            gradleEnterprise {
                server = "$develocityUrl"
                accessKey="$develocityAccessKey"
                buildScan {
                      isUploadInBackground = false
                      tag("$randomValue")

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
            }
            """,
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()

        val gcLogs = mutableListOf<Value>()
        runBlocking {
            val client = HttpClient(CIO) {}
            try {
                delay(10000)
                val response =
                    client.get("$develocityUrl/api/builds") {
                        header("Authorization", "Bearer $develocityAccessKey")
                        parameter("maxBuilds", 1)
                        parameter("models", "gradle-attributes")
                        parameter("reverse", true)
                        parameter("query", "tag:$randomValue")
                    }
                val responseBody: String = response.body()
                val gson = Gson()
                println(responseBody)
                val responseArray = gson.fromJson(responseBody, Array<Response>::class.java)
                gcLogs.addAll(responseArray[0].models.gradleAttributes.model.values)
            } catch (e: Exception) {
                println(e)
            }
        }
        assertTrue(gcLogs.isNotEmpty())
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-total-collections" })
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-Pause Young (Normal) (G1 Evacuation Pause)" })
    }

    @Test
    fun `plugin generates Output with Gc report for Parallel`() {
        Assume.assumeTrue(
            "Develocity URL and Access Key are set",
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null,
        )

        val develocityUrl = System.getenv("GE_URL")
        val develocityAccessKey = System.getenv("GE_API_KEY")
        val gcFile = "gc.log"

        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/$gcFile"
        gradleProperties.writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog -XX:+UseParallelGC
            """.trimIndent(),
        )
        val settingsGradle = File(testProjectDir, "settings.gradle.kts")
        val randomValue = Random.nextInt(Int.MAX_VALUE).toString()

        settingsGradle.writeText(
            """
            plugins {
                id("com.gradle.develocity") version "3.19"
            }
            gradleEnterprise {
                server = "$develocityUrl"
                accessKey="$develocityAccessKey"
                buildScan {
                      isUploadInBackground = false
                      tag("$randomValue")

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
            }
            """,
        )

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()

        val gcLogs = mutableListOf<Value>()
        runBlocking {
            val client = HttpClient(CIO) {}
            try {
                delay(10000)
                val response =
                    client.get("$develocityUrl/api/builds") {
                        header("Authorization", "Bearer $develocityAccessKey")
                        parameter("maxBuilds", 1)
                        parameter("models", "gradle-attributes")
                        parameter("reverse", true)
                        parameter("query", "tag:$randomValue")
                    }
                val responseBody: String = response.body()
                val gson = Gson()
                val responseArray = gson.fromJson(responseBody, Array<Response>::class.java)
                gcLogs.addAll(responseArray[0].models.gradleAttributes.model.values)
            } catch (e: Exception) {
                println(e)
            }
        }
        assertTrue(gcLogs.isNotEmpty())
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-total-collections" })
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-Pause Full (Metadata GC Threshold)" })
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-Pause Young (Metadata GC Threshold)" })
    }

    @Test
    fun `plugin generates Output with Gc report and Histogram`() {
        Assume.assumeTrue(
            "Develocity URL and Access Key are set",
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null,
        )

        val develocityUrl = System.getenv("GE_URL")
        val develocityAccessKey = System.getenv("GE_API_KEY")
        val gcFile = "gc.log"
        val settingsGradle = File(testProjectDir, "settings.gradle.kts")
        val randomValue = Random.nextInt(Int.MAX_VALUE).toString()

        settingsGradle.writeText(
            """
            plugins {
                id("com.gradle.develocity") version "3.19"
            }
            gradleEnterprise {
                server = "$develocityUrl"
                accessKey="$develocityAccessKey"
                buildScan {
                      isUploadInBackground = false
                      tag("$randomValue")

                }
            }
            """.trimIndent(),
        )

        val gradleProperties = File(testProjectDir, "gradle.properties")
        val gcLog = "${testProjectDir.absolutePath}/$gcFile"
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

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()
        val gcLogs = mutableListOf<Value>()
        runBlocking {
            val client = HttpClient(CIO) {}
            try {
                delay(10000)
                val response =
                    client.get("$develocityUrl/api/builds") {
                        header("Authorization", "Bearer $develocityAccessKey")
                        parameter("maxBuilds", 1)
                        parameter("models", "gradle-attributes")
                        parameter("reverse", true)
                        parameter("query", "tag:$randomValue")
                    }
                val responseBody: String = response.body()
                println(responseBody)
                val gson = Gson()
                val responseArray = gson.fromJson(responseBody, Array<Response>::class.java)
                gcLogs.addAll(responseArray[0].models.gradleAttributes.model.values)
            } catch (e: Exception) {
                println(e)
            }
        }
        assertTrue(gcLogs.isNotEmpty())
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-total-collections" })
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-histogram" })
    }
}
