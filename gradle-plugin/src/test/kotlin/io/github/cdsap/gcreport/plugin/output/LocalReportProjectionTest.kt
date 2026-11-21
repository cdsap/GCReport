package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocalReportProjectionTest {
    @Test
    fun `projects exact collection csv and console table`() {
        val projection = LocalReportProjection.project("gc.log", metrics(), histogramBucket = null)

        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n",
            projection.collectionCsv,
        )
        assertNull(projection.histogramCsv)
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
            projection.consoleTables,
        )
    }

    @Test
    fun `projects exact histogram csv and console table`() {
        val projection =
            LocalReportProjection.project(
                "gc.log",
                GCReportMetrics.from(
                    metricsEntries() + gcEntry("4.00", GCReportMetrics.CONCURRENT_MARK_CYCLE),
                ),
                Bucket.SquareRoot,
            )

        assertEquals(
            "Bucket,Occurrences\n" +
                "0.00-1.00,0\n" +
                "1.00-2.00,1\n" +
                "2.00-3.00,1\n" +
                "3.00-End,1\n",
            projection.histogramCsv,
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
            projection.consoleTables[1],
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
