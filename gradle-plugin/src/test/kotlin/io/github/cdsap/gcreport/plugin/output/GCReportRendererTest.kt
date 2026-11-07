package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GCReportRendererTest {
    @Test
    fun `renders collection csv and console report`() {
        val report = renderer(histogramEnabled = false).render("gc.log", metrics())

        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n",
            report.collectionCsv,
        )
        assertNull(report.histogramCsv)
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
            report.consoleReports,
        )
    }

    @Test
    fun `renders histogram csv and console report when enabled`() {
        val report = renderer(histogramEnabled = true).render("gc.log", metrics())

        assertEquals(
            "Bucket,Occurrences\n" +
                "0.00-1.00,0\n" +
                "1.00-2.00,1\n" +
                "2.00-3.00,1\n" +
                "3.00-End,1\n",
            report.histogramCsv,
        )
        assertEquals(2, report.consoleReports.size)
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
            report.consoleReports[1],
        )
    }

    private fun renderer(histogramEnabled: Boolean) =
        GCReportRenderer(
            GCReportRenderingOptions(
                histogramEnabled = histogramEnabled,
                histogramBucket = Bucket.SquareRoot,
            ),
        )

    private fun metrics() =
        GCReportMetrics.from(
            listOf(
                gcEntry("1.00", "Pause Young (Normal)"),
                gcEntry("2.00", "Pause Young (Normal)"),
                gcEntry("3.00", "Pause Full (System.gc())"),
            ),
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
