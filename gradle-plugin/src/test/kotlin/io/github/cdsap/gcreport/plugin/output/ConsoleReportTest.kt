package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConsoleReportTest {
    @Test
    fun `renders collection table from gc entries`() {
        val report = ConsoleReport(histogramEnabled = false, histogramBucket = Bucket.SquareRoot)

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
            report.render("gc.log", entries()),
        )
    }

    @Test
    fun `renders histogram table when enabled`() {
        val report = ConsoleReport(histogramEnabled = true, histogramBucket = Bucket.SquareRoot)

        assertEquals(2, report.render("gc.log", entries()).size)
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
            report.render("gc.log", entries())[1],
        )
    }

    private fun entries() =
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
