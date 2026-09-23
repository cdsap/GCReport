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
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.random.Random

class GCReportPluginWithDevelocityTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin takes Develocity path without requiring live server credentials`() {
        val gcFile = "gc.log"
        val gcLog = "${testProjectDir.absolutePath}/$gcFile"
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
                gcReportBody = """logs.set(listOf("$gcLog"))""",
            ),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks")
                .withPluginClasspath()
                .build()

        // DevelocityConfiguration is present, so GCReportPlugin takes the Develocity branch
        // (console/CSV reporting stays off unless enableConsoleLog is explicitly enabled).
        assertFalse(result.output.contains("GC Log: gc.log"))
        assertFalse(result.output.contains("Collection type"))
        assertFalse(testProjectDir.resolve("build/reports/gcreport/gc.csv").exists())
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `plugin generates Output with Gc report for G1`() {
        assumeTrue(
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null,
            "Develocity URL and Access Key are set",
        )
        val develocityUrl = System.getenv("GE_URL")
        val develocityAccessKey = System.getenv("GE_API_KEY")

        val gcFile = "gc.log"
        val gcLog = "${testProjectDir.absolutePath}/$gcFile"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        val randomValue = Random.nextInt(Int.MAX_VALUE).toString()

        File(testProjectDir, "settings.gradle.kts").writeText(
            DevelocityTestSupport.settingsScript(
                configureBody =
                    """
                    server.set("$develocityUrl")
                    accessKey.set("$develocityAccessKey")
                    buildScan {
                          uploadInBackground = false
                          tag("$randomValue")
                    }
                    """.trimIndent(),
                includeGcReport = true,
                gcReportBody = """logs.set(listOf("$gcLog"))""",
            ),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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
                    client.get("${develocityUrl}api/builds") {
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
        assumeTrue(
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null,
            "Develocity URL and Access Key are set",
        )

        val develocityUrl = System.getenv("GE_URL")
        val develocityAccessKey = System.getenv("GE_API_KEY")
        val gcFile = "gc.log"

        val gcLog = "${testProjectDir.absolutePath}/$gcFile"
        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog, " -XX:+UseParallelGC")
        val randomValue = Random.nextInt(Int.MAX_VALUE).toString()

        File(testProjectDir, "settings.gradle.kts").writeText(
            DevelocityTestSupport.settingsScript(
                configureBody =
                    """
                    server.set("$develocityUrl")
                    accessKey.set("$develocityAccessKey")
                    buildScan {
                          uploadInBackground = false
                          tag("$randomValue")
                    }
                    """.trimIndent(),
                includeGcReport = true,
                gcReportBody = """logs.set(listOf("$gcLog"))""",
            ),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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
                    client.get("${develocityUrl}api/builds") {
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
        assumeTrue(
            System.getenv("GE_URL") != null && System.getenv("GE_API_KEY") != null,
            "Develocity URL and Access Key are set",
        )

        val develocityUrl = System.getenv("GE_URL")
        val develocityAccessKey = System.getenv("GE_API_KEY")
        val gcFile = "gc.log"
        val randomValue = Random.nextInt(Int.MAX_VALUE).toString()
        val gcLog = "${testProjectDir.absolutePath}/$gcFile"

        File(testProjectDir, "settings.gradle.kts").writeText(
            DevelocityTestSupport.settingsScript(
                configureBody =
                    """
                    server.set("$develocityUrl")
                    accessKey.set("$develocityAccessKey")
                    buildScan {
                          uploadInBackground = false
                          tag("$randomValue")
                    }
                    """.trimIndent(),
                includeGcReport = true,
                gcReportBody =
                    """
                    logs.set(listOf("$gcLog"))
                    histogramEnabled.set(true)
                    histogramBucket.set(io.github.cdsap.gcreport.plugin.model.Bucket.SquareRoot)
                    """.trimIndent(),
            ),
        )

        GCReportTestFixtures.writeJvmGcProperties(testProjectDir, gcLog)
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

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
                    client.get("${develocityUrl}api/builds") {
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
        assertTrue(gcLogs.any { it.name == "gc-$gcFile-histogram" })
    }

    @Test
    fun `plugin applies alongside Develocity with compileOnly classpath`() {
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
                gcReportBody = """logs.set(listOf("$gcLog"))""",
            ),
        )
        GCReportTestFixtures.writeMinimalBuild(testProjectDir)

        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }
}
