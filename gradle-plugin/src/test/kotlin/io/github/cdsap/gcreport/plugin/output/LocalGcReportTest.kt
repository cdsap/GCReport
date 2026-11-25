package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LocalGcReportTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private val buildOutput get() = File(temporaryDirectory, "build/reports/gcreport")

    @Test
    fun `writes exact collection csv and logs console table without histogram`() {
        val consoleTables = mutableListOf<String>()
        LocalGcReport(consoleTables::add).report(
            metrics(),
            "/logs/gc.log",
            buildOutput,
            histogramBucket = null,
        )

        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n",
            File(buildOutput, "gc.csv").readText(),
        )
        assertFalse(File(buildOutput, "histogram_gc.csv").exists())
        assertEquals(listOf("gc.csv"), buildOutput.list()!!.toList())
        assertEquals(
            listOf(
                """
                ┌────────────────────────────────────────┐
                │ GC Log: gc.log                         │
                ├──────────────────────────┬─────────────┤
                │ Collection type          │ Occurrences │
                ├──────────────────────────┼─────────────┤
                │ Pause Young (Normal)     │           2 │
                ├──────────────────────────┼─────────────┤
                │ Pause Full (System.gc()) │           1 │
                └──────────────────────────┴─────────────┘
                """.trimIndent(),
            ),
            consoleTables,
        )
    }

    @Test
    fun `writes exact histogram csv and logs tables in order`() {
        val consoleTables = mutableListOf<String>()
        LocalGcReport(consoleTables::add).report(
            GCReportMetrics.from(
                metricsEntries() + gcEntry("4.00", GCReportMetrics.CONCURRENT_MARK_CYCLE),
            ),
            "/logs/gc.log",
            buildOutput,
            Bucket.SquareRoot,
        )

        assertEquals(
            setOf("gc.csv", "histogram_gc.csv"),
            buildOutput.list()!!.toSet(),
        )
        assertEquals(2, consoleTables.size)
        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n" +
                "Concurrent Mark Cycle,1\n",
            File(buildOutput, "gc.csv").readText(),
        )
        assertEquals("│ GC Log: gc.log                         │", consoleTables[0].lines()[1])

        assertEquals(
            "Bucket,Occurrences\n" +
                "0.00-1.00,0\n" +
                "1.00-2.00,1\n" +
                "2.00-3.00,1\n" +
                "3.00-End,1\n",
            File(buildOutput, "histogram_gc.csv").readText(),
        )
        assertEquals(
            """
            ┌─────────────────────────┐
            │ GC Histogram: gc.log    │
            ├─────────────────────────┤
            │ Type: SquareRoot        │
            ├───────────┬─────────────┤
            │ Bucket    │ Occurrences │
            ├───────────┼─────────────┤
            │ 0.00-1.00 │           0 │
            ├───────────┼─────────────┤
            │ 1.00-2.00 │           1 │
            ├───────────┼─────────────┤
            │ 2.00-3.00 │           1 │
            ├───────────┼─────────────┤
            │ 3.00-End  │           1 │
            └───────────┴─────────────┘
            """.trimIndent(),
            consoleTables[1],
        )
    }

    private fun metrics() = GCReportMetrics.from(metricsEntries())

    private fun metricsEntries() =
        listOf(
            gcEntry("1.00", "Pause Young (Normal)"),
            gcEntry("2.00", "Pause Young (Normal)"),
            gcEntry("3.00", "Pause Full (System.gc())"),
        )

    private fun gcEntry(
        timeStamp: String,
        description: String,
    ): GCEntry =
        GCEntry(
            timeStamp = timeStamp,
            timeStampUnit = "s",
            id = timeStamp,
            description = description,
            duration = "10",
            durationUnit = "ms",
        )
}
