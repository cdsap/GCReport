package io.github.cdsap.gcreport.plugin.output

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CsvReportProjectionTest {
    @Test
    fun `projects collection counts to the exact csv report`() {
        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n",
            CsvReportProjection.collection(
                linkedMapOf(
                    "Pause Young (Normal)" to 2,
                    "Pause Full (System.gc())" to 1,
                ),
            ),
        )
    }

    @Test
    fun `projects histogram entries to the exact csv report`() {
        assertEquals(
            "Bucket,Occurrences\n" +
                "0.00-1.00,0\n" +
                "1.00-2.00,1\n" +
                "2.00-End,2\n",
            CsvReportProjection.histogram(
                listOf(
                    "0.00-1.00" to 0,
                    "1.00-2.00" to 1,
                    "2.00-End" to 2,
                ),
            ),
        )
    }

    @Test
    fun `preserves collection labels without escaping`() {
        assertEquals(
            "Collection type,Occurrences\nlabel,with comma,0\n",
            CsvReportProjection.collection(linkedMapOf("label,with comma" to 0)),
        )
    }

    @Test
    fun `projects empty reports with trailing newlines`() {
        assertEquals("Collection type,Occurrences\n", CsvReportProjection.collection(emptyMap()))
        assertEquals("Bucket,Occurrences\n", CsvReportProjection.histogram(emptyList()))
    }
}
