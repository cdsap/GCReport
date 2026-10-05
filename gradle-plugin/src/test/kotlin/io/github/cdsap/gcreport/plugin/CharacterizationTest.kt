package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.fake.BuildResultBehaviour
import io.github.cdsap.gcreport.plugin.fake.FakeDevelocityPlugin
import io.github.cdsap.gcreport.plugin.fake.FakeGradleEnterprisePlugin
import io.github.cdsap.gcreport.plugin.fake.FakeProxies
import io.github.cdsap.gcreport.plugin.fake.ScanSink
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.report.DevelocitySupport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Action
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Locale
import java.util.Properties
import com.gradle.develocity.agent.gradle.scan.BuildResult as DevelocityBuildResult

/**
 * Characterization tests: pin what consumers of `io.github.cdsap.gcreport` observe today (Build
 * Scan custom values, console report, `build/reports/gcreport` CSV files) so later migrations onto
 * shared libraries can prove they are behaviour-preserving. Expected values are recorded from the
 * current implementation, not derived from a spec.
 *
 * Input is always `src/test/resources/correct_g1_log`, copied into the project as `gc.log` and
 * configured through `gcReport.logs`. TestKit daemons get no `-Xlog:gc` argument, so nothing
 * overwrites the file and every run sees the same 19 G1 events.
 *
 * Levels:
 * - Unit: `DevelocitySupport.register` (the hard cast) -> `DevelocityReport` -> `DevelocityValues`
 *   with a recording [ScanSink] behind the same Develocity proxy the fake plugin uses.
 * - TestKit: [FakeDevelocityPlugin] stands in for `com.gradle.develocity` and prints
 *   `SCAN-VALUE <name>=<value>` lines from its `buildFinished` replay.
 *
 * Histogram buckets are formatted with the default locale (`String.format("%.2f")`), so the unit
 * level pins `Locale.US` and TestKit daemons get `-Duser.language=en -Duser.country=US`.
 */
class CharacterizationTest {
    @TempDir
    lateinit var testProjectDir: File

    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun pinLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    // ---------------------------------------------------------------- unit level

    @Test
    fun `unit - correct_g1_log emits one value per collection type in first-seen order then total-collections`() {
        assertEquals(EXPECTED_LEGACY_SCAN_LINES, unitScanLines {})
    }

    @Test
    fun `unit - histogramEnabled appends the histogram value after total-collections`() {
        assertEquals(
            EXPECTED_LEGACY_SCAN_LINES + EXPECTED_FREEDMAN_DIACONIS_HISTOGRAM_LINE,
            unitScanLines { histogramEnabled.set(true) },
        )
        assertEquals(
            EXPECTED_LEGACY_SCAN_LINES + EXPECTED_SQUARE_ROOT_HISTOGRAM_LINE,
            unitScanLines {
                histogramEnabled.set(true)
                histogramBucket.set(Bucket.SquareRoot)
            },
        )
    }

    @Test
    fun `unit - gbosEnabled keeps the legacy values and appends one observation per collection type`() {
        val entries = parseScanValues(unitScanLines { gbosEnabled.set(true) })

        assertEquals(EXPECTED_LEGACY_KEYS + List(6) { GBOS_OBSERVATION_KEY }, entries.map { it.first })
        assertEquals(EXPECTED_LEGACY_SCAN_LINES, entries.take(7).map { "SCAN-VALUE ${it.first}=${it.second}" })
        assertEquals(EXPECTED_OBSERVATIONS, entries.drop(7).map { describeObservation(it.second) })
    }

    @Test
    fun `unit - histogram and gbos together order legacy values, histogram, then observations`() {
        val entries =
            parseScanValues(
                unitScanLines {
                    histogramEnabled.set(true)
                    gbosEnabled.set(true)
                },
            )

        assertEquals(
            EXPECTED_LEGACY_KEYS + HISTOGRAM_KEY + List(6) { GBOS_OBSERVATION_KEY },
            entries.map { it.first },
        )
    }

    // ---------------------------------------------------------------- TestKit level, fake Develocity

    @Test
    fun `fake Develocity in settings - default config emits legacy values only, no console report or CSV`() {
        writeProject(settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID))

        val result = run()

        assertFakeDevelocityApplied(result)
        assertEquals(EXPECTED_LEGACY_SCAN_LINES, scanLines(result))
        assertEquals(emptyList<String>(), consoleTableLines(result))
        assertFalse(reportsDir().exists(), "no CSV reports without enableConsoleLog")
    }

    @Test
    fun `fake Develocity in settings - histogramEnabled appends the histogram value`() {
        writeProject(settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID), gcReportBody = "histogramEnabled.set(true)")

        val result = run()

        assertFakeDevelocityApplied(result)
        assertEquals(EXPECTED_LEGACY_SCAN_LINES + EXPECTED_FREEDMAN_DIACONIS_HISTOGRAM_LINE, scanLines(result))
        assertEquals(emptyList<String>(), consoleTableLines(result))
        assertFalse(reportsDir().exists())
    }

    @Test
    fun `fake Develocity in settings - gbosEnabled keeps legacy values and appends observations`() {
        writeProject(settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID), gcReportBody = "gbosEnabled.set(true)")

        val result = run()

        assertFakeDevelocityApplied(result)
        val entries = parseScanValues(result.output.lines())
        assertEquals(EXPECTED_LEGACY_KEYS + List(6) { GBOS_OBSERVATION_KEY }, entries.map { it.first })
        assertEquals(EXPECTED_LEGACY_SCAN_LINES, entries.take(7).map { "SCAN-VALUE ${it.first}=${it.second}" })
        assertEquals(EXPECTED_OBSERVATIONS, entries.drop(7).map { describeObservation(it.second) })
    }

    @Test
    fun `fake Develocity in settings - enableConsoleLog adds console tables and CSV next to scan values`() {
        writeProject(
            settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID),
            gcReportBody = "enableConsoleLog.set(true)\n    histogramEnabled.set(true)",
        )

        val result = run()

        assertFakeDevelocityApplied(result)
        assertEquals(EXPECTED_LEGACY_SCAN_LINES + EXPECTED_FREEDMAN_DIACONIS_HISTOGRAM_LINE, scanLines(result))
        assertEquals(EXPECTED_COLLECTION_TABLE + EXPECTED_HISTOGRAM_TABLE, consoleTableLines(result))
        assertEquals(
            mapOf("gc.csv" to EXPECTED_GC_CSV, "histogram_gc.csv" to EXPECTED_HISTOGRAM_CSV),
            reportFiles(),
        )
    }

    @Test
    fun `fake Develocity declared before or after GCReport in settings plugins yields identical scan values`() {
        val config = "histogramEnabled.set(true)\n    gbosEnabled.set(true)"

        writeProject(settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID), gcReportBody = config)
        val before = run()
        writeProject(settingsPlugins = listOf(GCREPORT_ID, DEVELOCITY_ID), gcReportBody = config)
        val after = run()

        assertFakeDevelocityApplied(before)
        assertFakeDevelocityApplied(after)
        val beforeEntries = parseScanValues(before.output.lines())
        assertEquals(EXPECTED_LEGACY_KEYS + HISTOGRAM_KEY + List(6) { GBOS_OBSERVATION_KEY }, beforeEntries.map { it.first })
        assertEquals(beforeEntries, parseScanValues(after.output.lines()))
    }

    @Test
    fun `legacy root build script application with fake Develocity in settings takes the Develocity path`() {
        // Current behaviour: applied to a Project, GCReport only looks for a Develocity extension on
        // the root project (GCReportPluginSupport.applyToProject). Develocity (real 4.x and the fake)
        // adds its settings `develocity` instance to the root project, so the lookup succeeds:
        // legacy scan values are emitted and the console/CSV report stays off (enableConsoleLog unset).
        writeProject(
            settingsPlugins = listOf(DEVELOCITY_ID),
            gcReportBody = null,
            rootBuildScriptGcReport = true,
            extraBuildScript = PRINT_ROOT_DEVELOCITY,
        )

        val result = run()

        assertFakeDevelocityApplied(result)
        assertTrue(result.output.contains("$ROOT_DEVELOCITY_PREFIX true"), result.output)
        assertEquals(EXPECTED_LEGACY_SCAN_LINES, scanLines(result))
        assertEquals(emptyList<String>(), consoleTableLines(result))
        assertFalse(reportsDir().exists(), "no CSV reports without enableConsoleLog")
    }

    @Test
    fun `GCReport in settings and root build script with fake Develocity reports once`() {
        writeProject(
            settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID),
            gcReportBody = "enableConsoleLog.set(true)",
            rootBuildScriptGcReport = true,
        )

        val result = run()

        assertFakeDevelocityApplied(result)
        assertEquals(EXPECTED_LEGACY_SCAN_LINES, scanLines(result))
        assertEquals(EXPECTED_COLLECTION_TABLE, consoleTableLines(result))
        assertEquals(mapOf("gc.csv" to EXPECTED_GC_CSV), reportFiles())
    }

    @Test
    fun `configuration cache reuse replays identical scan values, console report and CSV`() {
        writeProject(settingsPlugins = listOf(DEVELOCITY_ID, GCREPORT_ID), gcReportBody = "enableConsoleLog.set(true)")

        val store = run("--configuration-cache")
        assertTrue(store.output.contains("Configuration cache entry stored."), store.output)
        assertFakeDevelocityApplied(store)
        val storeReports = reportFiles()
        reportsDir().deleteRecursively()

        val reuse = run("--configuration-cache")
        assertTrue(reuse.output.contains("Reusing configuration cache."), reuse.output)
        // Settings are not evaluated on reuse, so the fake's applied marker only prints on store.
        assertFalse(reuse.output.contains(FakeDevelocityPlugin.APPLIED_MARKER), reuse.output)

        assertEquals(EXPECTED_LEGACY_SCAN_LINES, scanLines(store))
        assertEquals(scanLines(store), scanLines(reuse))
        assertEquals(EXPECTED_COLLECTION_TABLE, consoleTableLines(store))
        assertEquals(consoleTableLines(store), consoleTableLines(reuse))
        assertEquals(mapOf("gc.csv" to EXPECTED_GC_CSV), storeReports)
        assertEquals(storeReports, reportFiles())
    }

    // ---------------------------------------------------------------- TestKit level, no Develocity

    @Test
    fun `no Develocity - console report and CSV are written whether enableConsoleLog is on or off`() {
        // Current behaviour: without Develocity the report is always enabled; enableConsoleLog is ignored.
        for (enableConsoleLog in listOf(false, true)) {
            reportsDir().deleteRecursively()
            writeProject(settingsPlugins = listOf(GCREPORT_ID), gcReportBody = "enableConsoleLog.set($enableConsoleLog)")

            val result = run()

            assertFalse(result.output.contains(FakeDevelocityPlugin.APPLIED_MARKER))
            assertEquals(emptyList<String>(), scanLines(result), "enableConsoleLog=$enableConsoleLog")
            assertEquals(EXPECTED_COLLECTION_TABLE, consoleTableLines(result), "enableConsoleLog=$enableConsoleLog")
            assertEquals(mapOf("gc.csv" to EXPECTED_GC_CSV), reportFiles(), "enableConsoleLog=$enableConsoleLog")
        }
    }

    @Test
    fun `no Develocity - histogramEnabled adds the histogram table and CSV`() {
        writeProject(settingsPlugins = listOf(GCREPORT_ID), gcReportBody = "histogramEnabled.set(true)")

        val result = run()

        assertEquals(emptyList<String>(), scanLines(result))
        assertEquals(EXPECTED_COLLECTION_TABLE + EXPECTED_HISTOGRAM_TABLE, consoleTableLines(result))
        assertEquals(
            mapOf("gc.csv" to EXPECTED_GC_CSV, "histogram_gc.csv" to EXPECTED_HISTOGRAM_CSV),
            reportFiles(),
        )
    }

    // ---------------------------------------------------------------- recorded, not pinned
    //
    // The following outcomes are expected to change on purpose (the hard cast to
    // DevelocityConfiguration in DevelocitySupport). They only print what happens so the change is
    // visible in the test log; they never fail on either outcome.

    @Test
    fun `RECORDED ONLY - legacy Gradle Enterprise plugin (fake com_gradle_enterprise) in settings`() {
        writeProject(settingsPlugins = listOf(GRADLE_ENTERPRISE_ID, GCREPORT_ID), extraSettings = PRINT_EXTENSION_SCHEMA)

        val result = runTolerant(testKitClasspath(), "--stacktrace")

        record("fake com.gradle.enterprise + gcReport in settings", result)
        assertTrue(result.output.contains(FakeGradleEnterprisePlugin.APPLIED_MARKER), result.output)
    }

    @Test
    fun `RECORDED ONLY - legacy Gradle Enterprise plugin (real com_gradle_enterprise from the Develocity jar) in settings`() {
        writeProject(settingsPlugins = listOf(GRADLE_ENTERPRISE_ID, GCREPORT_ID), extraSettings = PRINT_EXTENSION_SCHEMA)

        record("real com.gradle.enterprise + gcReport in settings", runTolerant(pluginUnderTestClasspath(), "--stacktrace"))
    }

    @Test
    fun `RECORDED ONLY - Develocity injected by an init script`() {
        // Mirrors CI injection: the Develocity plugin comes from the init script classpath, so its
        // DevelocityConfiguration is a different Class than the one GCReport sees.
        writeProject(settingsPlugins = listOf(GCREPORT_ID))
        val initClasspath =
            listOf(fakeClassesDir()) + pluginUnderTestClasspath().filter { it.isFile && it.name.endsWith(".jar") }
        val initScript =
            File(testProjectDir, "inject-develocity.gradle").apply {
                writeText(
                    """
                    initscript {
                        dependencies {
                            classpath files(${initClasspath.joinToString(", ") { "'${it.absolutePath}'" }})
                        }
                    }
                    beforeSettings { settings ->
                        settings.pluginManager.apply(io.github.cdsap.gcreport.plugin.fake.FakeDevelocityPlugin)
                    }
                    """.trimIndent(),
                )
            }

        val result = runTolerant(pluginUnderTestClasspath(), "--init-script", initScript.absolutePath, "--stacktrace")

        record("init-script-injected fake com.gradle.develocity + gcReport in settings", result)
        assertTrue(result.output.contains(FakeDevelocityPlugin.APPLIED_MARKER), result.output)
    }

    // ---------------------------------------------------------------- helpers

    private fun unitScanLines(configure: GCReportExtension.() -> Unit): List<String> {
        val project =
            ProjectBuilder.builder()
                .withProjectDir(File(testProjectDir, "unit-project").apply { mkdirs() })
                .build()
        val extension = GCReportPluginSupport.createExtension(project)
        extension.logs.set(listOf(copyGcLog().absolutePath))
        extension.configure()

        val lines = mutableListOf<String>()
        val finishedActions = mutableListOf<Any>()
        val develocity = FakeProxies.develocity(project.objects, finishedActions, ScanSink { lines += it })
        DevelocitySupport.register(develocity, extension)

        assertEquals(emptyList<String>(), lines, "values are only written from buildFinished")
        assertEquals(1, finishedActions.size)
        @Suppress("UNCHECKED_CAST")
        (finishedActions.single() as Action<Any>).execute(
            FakeProxies.create(DevelocityBuildResult::class.java, null, BuildResultBehaviour()),
        )
        return lines
    }

    private fun copyGcLog(): File {
        val target = File(testProjectDir, GC_LOG_NAME)
        if (!target.exists()) {
            checkNotNull(javaClass.getResourceAsStream("/correct_g1_log")).use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }
        return target
    }

    private fun writeProject(
        settingsPlugins: List<String>,
        gcReportBody: String? = "",
        rootBuildScriptGcReport: Boolean = false,
        extraSettings: String = "",
        extraBuildScript: String = "",
    ) {
        val gcLog = copyGcLog().absolutePath
        File(testProjectDir, "gradle.properties").writeText(DAEMON_PROPERTIES)
        File(testProjectDir, "settings.gradle.kts").writeText(
            buildString {
                appendLine("plugins {")
                settingsPlugins.forEach { appendLine("    id(\"$it\")") }
                appendLine("}")
                if (gcReportBody != null) {
                    appendLine("gcReport {")
                    appendLine("    logs.set(listOf(\"$gcLog\"))")
                    appendLine("    $gcReportBody")
                    appendLine("}")
                }
                appendLine(extraSettings)
            },
        )
        File(testProjectDir, "build.gradle.kts").writeText(
            buildString {
                if (rootBuildScriptGcReport) {
                    appendLine("plugins {")
                    appendLine("    id(\"$GCREPORT_ID\")")
                    appendLine("}")
                    appendLine("gcReport {")
                    appendLine("    logs.set(listOf(\"$gcLog\"))")
                    appendLine("}")
                }
                appendLine(extraBuildScript)
                appendLine("tasks.register(\"$TASK\")")
            },
        )
    }

    private fun run(vararg extraArgs: String): BuildResult {
        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath(testKitClasspath())
                .withArguments(listOf(TASK) + extraArgs)
                .build()
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":$TASK")?.outcome)
        return result
    }

    private fun runTolerant(
        classpath: List<File>,
        vararg extraArgs: String,
    ): BuildResult =
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath(classpath)
            .withArguments(listOf(TASK) + extraArgs)
            .run()

    private fun record(
        scenario: String,
        result: BuildResult,
    ) {
        val failed = result.output.contains("BUILD FAILED")
        val cause =
            result.output.lines()
                .firstOrNull { it.contains("ClassCastException") || it.contains("Exception:") }
                ?.trim()
        println("RECORDED [$scenario]: ${if (failed) "BUILD FAILED" else "BUILD SUCCESSFUL"}")
        cause?.let { println("RECORDED [$scenario]: first exception line: $it") }
        result.output.lines().filter { it.startsWith(EXTENSION_SCHEMA_PREFIX) }.forEach {
            println("RECORDED [$scenario]: $it")
        }
        println("RECORDED [$scenario]: scan values: ${scanLines(result)}")
        println("RECORDED [$scenario]: console report lines: ${consoleTableLines(result).size}")
        println("RECORDED [$scenario]: report files: ${reportFiles().keys}")
    }

    private fun assertFakeDevelocityApplied(result: BuildResult) {
        assertTrue(result.output.contains(FakeDevelocityPlugin.APPLIED_MARKER), result.output)
    }

    private fun reportsDir(): File = testProjectDir.resolve("build/reports/gcreport")

    private fun reportFiles(): Map<String, String> =
        reportsDir().listFiles().orEmpty().sortedBy { it.name }.associate { it.name to it.readText() }

    /** Single-line `SCAN-VALUE`/`SCAN-TAG` output; multi-line values are folded by [parseScanValues]. */
    private fun scanLines(result: BuildResult): List<String> = result.output.lines().filter { it.startsWith("SCAN-") }

    /** picnic tables printed by `GCReportService`. */
    private fun consoleTableLines(result: BuildResult): List<String> =
        result.output.lines().filter { line -> line.isNotEmpty() && line[0] in "┌│├└" }

    /**
     * `(name, value)` pairs from `SCAN-VALUE` lines. GBOS observation values are pretty-printed JSON
     * spanning several lines; they run until the closing `}` at column 0.
     */
    private fun parseScanValues(lines: List<String>): List<Pair<String, String>> {
        val entries = mutableListOf<Pair<String, String>>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index++]
            if (!line.startsWith("SCAN-VALUE ")) continue
            val name = line.removePrefix("SCAN-VALUE ").substringBefore("=")
            val value = StringBuilder(line.substringAfter("="))
            if (value.toString() == "{") {
                while (index < lines.size) {
                    val continuation = lines[index++]
                    value.append('\n').append(continuation)
                    if (continuation == "}") break
                }
            }
            entries += name to value.toString()
        }
        return entries
    }

    private fun describeObservation(raw: String): String {
        val observation: JsonObject = Json.parseToJsonElement(raw).jsonObject
        assertEquals(
            listOf("schemaVersion", "producer", "scope", "aggregationScope", "attributes", "measurements"),
            observation.keys.toList(),
        )
        val producer = observation.getValue("producer").jsonObject
        assertEquals(listOf("name", "version"), producer.keys.toList())
        val attributes = observation.getValue("attributes").jsonObject
        assertEquals(listOf("log.file.name", "jvm.process.role", "jvm.gc.action"), attributes.keys.toList())
        val measurements =
            observation.getValue("measurements").jsonArray.map { element ->
                val measurement = element.jsonObject
                assertEquals(listOf("name", "value", "unit", "aggregation"), measurement.keys.toList())
                listOf(
                    measurement.getValue("name").jsonPrimitive.content,
                    measurement.getValue("value").jsonPrimitive.double.toString(),
                    measurement.getValue("unit").jsonPrimitive.content,
                    measurement.getValue("aggregation").jsonPrimitive.content,
                ).joinToString(" ")
            }
        return listOf(
            "schemaVersion=${observation.getValue("schemaVersion").jsonPrimitive.content}",
            "producer=${producer.getValue("name").jsonPrimitive.content}@${producer.getValue("version").jsonPrimitive.content}",
            "scope=${observation.getValue("scope").jsonPrimitive.content}",
            "aggregationScope=${observation.getValue("aggregationScope").jsonPrimitive.content}",
            "attributes=" + attributes.entries.joinToString(",") { (key, value) -> "$key:${value.jsonPrimitive.content}" },
            "measurements=" + measurements.joinToString(";"),
        ).joinToString(" | ")
    }

    private fun pluginUnderTestClasspath(): List<File> {
        val metadata = Properties()
        checkNotNull(javaClass.classLoader.getResourceAsStream("plugin-under-test-metadata.properties")).use {
            metadata.load(it)
        }
        return metadata.getProperty("implementation-classpath").split(File.pathSeparator).map(::File)
    }

    private fun fakeClassesDir(): File = File(FakeDevelocityPlugin::class.java.protectionDomain.codeSource.location.toURI())

    /**
     * The plugin-under-test classpath prefixed with the test resources (the `com.gradle.develocity`
     * and `com.gradle.enterprise` descriptors pointing at the fakes) and test classes. Prefixing makes
     * the fake descriptors win over the real Develocity jar's, which stays on the classpath because
     * the plugin and the fakes compile against its API.
     */
    private fun testKitClasspath(): List<File> {
        val descriptor =
            javaClass.classLoader.getResources(FAKE_DEVELOCITY_DESCRIPTOR).toList().single { it.protocol == "file" }
        val testResources = File(descriptor.toURI()).parentFile.parentFile.parentFile
        return listOf(testResources, fakeClassesDir()) + pluginUnderTestClasspath()
    }

    private companion object {
        const val GC_LOG_NAME = "gc.log"
        const val TASK = "characterize"
        const val DEVELOCITY_ID = "com.gradle.develocity"
        const val GRADLE_ENTERPRISE_ID = "com.gradle.enterprise"
        const val GCREPORT_ID = "io.github.cdsap.gcreport"
        const val FAKE_DEVELOCITY_DESCRIPTOR = "META-INF/gradle-plugins/com.gradle.develocity.properties"
        const val DAEMON_PROPERTIES = "org.gradle.jvmargs=-Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US\n"
        const val ROOT_DEVELOCITY_PREFIX = "ROOT-HAS-DEVELOCITY"
        const val PRINT_ROOT_DEVELOCITY =
            "println(\"$ROOT_DEVELOCITY_PREFIX \" + " +
                "(extensions.findByType(com.gradle.develocity.agent.gradle.DevelocityConfiguration::class.java) != null))"
        const val EXTENSION_SCHEMA_PREFIX = "SETTINGS-EXTENSIONS "
        const val PRINT_EXTENSION_SCHEMA =
            "println(\"$EXTENSION_SCHEMA_PREFIX\" + " +
                "extensions.extensionsSchema.elements.joinToString { \"\${it.name}:\${it.publicType.concreteClass.name}\" })"

        const val GBOS_OBSERVATION_KEY = "gbos.v1.producer.gc_report.observation"
        const val HISTOGRAM_KEY = "gc-gc.log-histogram"

        val EXPECTED_LEGACY_SCAN_LINES =
            listOf(
                "SCAN-VALUE gc-gc.log-Pause Young (Concurrent Start) (Metadata GC Threshold)=5",
                "SCAN-VALUE gc-gc.log-Pause Young (Normal) (G1 Evacuation Pause)=5",
                "SCAN-VALUE gc-gc.log-Pause Young (Concurrent Start) (G1 Evacuation Pause)=1",
                "SCAN-VALUE gc-gc.log-Pause Young (Prepare Mixed) (G1 Evacuation Pause)=1",
                "SCAN-VALUE gc-gc.log-Pause Young (Mixed) (G1 Evacuation Pause)=1",
                "SCAN-VALUE gc-gc.log-Concurrent Mark Cycle=6",
                "SCAN-VALUE gc-gc.log-total-collections=13",
            )

        val EXPECTED_LEGACY_KEYS =
            EXPECTED_LEGACY_SCAN_LINES.map { it.removePrefix("SCAN-VALUE ").substringBefore("=") }

        // Note the trailing `,]`: the builder drops only the final space of `"...", `.
        const val EXPECTED_FREEDMAN_DIACONIS_HISTOGRAM_LINE =
            "SCAN-VALUE gc-gc.log-histogram=[\"0.00-13.83\": \"8\", \"13.83-27.66\": \"3\", \"27.66-End\": \"2\",]"

        const val EXPECTED_SQUARE_ROOT_HISTOGRAM_LINE =
            "SCAN-VALUE gc-gc.log-histogram=[\"0.00-8.10\": \"8\", \"8.10-16.20\": \"0\", " +
                "\"16.20-24.30\": \"2\", \"24.30-32.40\": \"2\", \"32.40-End\": \"1\",]"

        // One observation per collection type, sorted by action name (unlike the legacy values).
        val EXPECTED_OBSERVATIONS =
            listOf(
                "Concurrent Mark Cycle" to 6,
                "Pause Young (Concurrent Start) (G1 Evacuation Pause)" to 1,
                "Pause Young (Concurrent Start) (Metadata GC Threshold)" to 5,
                "Pause Young (Mixed) (G1 Evacuation Pause)" to 1,
                "Pause Young (Normal) (G1 Evacuation Pause)" to 5,
                "Pause Young (Prepare Mixed) (G1 Evacuation Pause)" to 1,
            ).map { (action, count) ->
                "schemaVersion=1.0.0 | producer=gc-report@0.1.0 | scope=jvm.gc | aggregationScope=entity | " +
                    "attributes=log.file.name:gc.log,jvm.process.role:gradle-daemon,jvm.gc.action:$action | " +
                    "measurements=jvm.gc.events ${count.toDouble()} {collection} count"
            }

        val EXPECTED_COLLECTION_TABLE =
            """
            ┌──────────────────────────────────────────────────────────────────────┐
            │ GC Log: gc.log                                                       │
            ├────────────────────────────────────────────────────────┬─────────────┤
            │ Collection type                                        │ Occurrences │
            ├────────────────────────────────────────────────────────┼─────────────┤
            │ Pause Young (Concurrent Start) (Metadata GC Threshold) │           5 │
            ├────────────────────────────────────────────────────────┼─────────────┤
            │ Pause Young (Normal) (G1 Evacuation Pause)             │           5 │
            ├────────────────────────────────────────────────────────┼─────────────┤
            │ Pause Young (Concurrent Start) (G1 Evacuation Pause)   │           1 │
            ├────────────────────────────────────────────────────────┼─────────────┤
            │ Pause Young (Prepare Mixed) (G1 Evacuation Pause)      │           1 │
            ├────────────────────────────────────────────────────────┼─────────────┤
            │ Pause Young (Mixed) (G1 Evacuation Pause)              │           1 │
            ├────────────────────────────────────────────────────────┼─────────────┤
            │ Concurrent Mark Cycle                                  │           6 │
            └────────────────────────────────────────────────────────┴─────────────┘
            """.trimIndent().lines()

        val EXPECTED_HISTOGRAM_TABLE =
            """
            ┌───────────────────────────┐
            │ GC Histogram: gc.log      │
            ├───────────────────────────┤
            │ Type: FreedmanDiaconis    │
            ├─────────────┬─────────────┤
            │ Bucket      │ Occurrences │
            ├─────────────┼─────────────┤
            │ 0.00-13.83  │           8 │
            ├─────────────┼─────────────┤
            │ 13.83-27.66 │           3 │
            ├─────────────┼─────────────┤
            │ 27.66-End   │           2 │
            └─────────────┴─────────────┘
            """.trimIndent().lines()

        // The CSV lists every collection type (including Concurrent Mark Cycle) but no total row.
        const val EXPECTED_GC_CSV =
            "Collection type,Occurrences\n" +
                "Pause Young (Concurrent Start) (Metadata GC Threshold),5\n" +
                "Pause Young (Normal) (G1 Evacuation Pause),5\n" +
                "Pause Young (Concurrent Start) (G1 Evacuation Pause),1\n" +
                "Pause Young (Prepare Mixed) (G1 Evacuation Pause),1\n" +
                "Pause Young (Mixed) (G1 Evacuation Pause),1\n" +
                "Concurrent Mark Cycle,6\n"

        const val EXPECTED_HISTOGRAM_CSV =
            "Bucket,Occurrences\n" +
                "0.00-13.83,8\n" +
                "13.83-27.66,3\n" +
                "27.66-End,2\n"
    }
}
