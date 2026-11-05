package io.github.cdsap.gcreport.plugin.report

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GCReportFormatterTest {
    @Test
    fun `formats collection counts`() {
        val formatter = GCReportFormatter(Bucket.SquareRoot)

        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n",
            formatter.collectionCounts(metrics()),
        )
    }

    @Test
    fun `formats histogram`() {
        val formatter = GCReportFormatter(Bucket.SquareRoot)

        assertEquals(
            "Bucket,Occurrences\n" +
                "0.00-1.00,0\n" +
                "1.00-2.00,1\n" +
                "2.00-3.00,1\n" +
                "3.00-End,1\n",
            formatter.histogram(metrics()),
        )
    }

    private fun metrics() =
        GCReportMetrics.from(
            listOf(
                GCEntry("1.00", "s", "1", "Pause Young (Normal)", duration = "10", durationUnit = "ms"),
                GCEntry("2.00", "s", "2", "Pause Young (Normal)", duration = "10", durationUnit = "ms"),
                GCEntry("3.00", "s", "3", "Pause Full (System.gc())", duration = "10", durationUnit = "ms"),
            ),
        )
}
