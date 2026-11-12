package io.github.cdsap.gcreport.plugin.output

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CsvReportTest {
    @Test
    fun `formats ordered rows without escaping`() {
        assertEquals(
            "Collection type,Occurrences\nPause Young,2\nlabel,with comma,0\n",
            formatCsvReport("Collection type,Occurrences", listOf("Pause Young" to 2, "label,with comma" to 0)),
        )
    }

    @Test
    fun `formats histogram rows in order with a trailing newline`() {
        assertEquals(
            "Bucket,Occurrences\n0.00-1.00,0\n1.00-End,2\n",
            formatCsvReport("Bucket,Occurrences", listOf("0.00-1.00" to 0, "1.00-End" to 2)),
        )
    }

    @Test
    fun `formats empty collection and histogram reports with trailing newlines`() {
        assertEquals("Collection type,Occurrences\n", formatCsvReport("Collection type,Occurrences", emptyList()))
        assertEquals("Bucket,Occurrences\n", formatCsvReport("Bucket,Occurrences", emptyList()))
    }
}
