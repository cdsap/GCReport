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
    fun `formats an empty report with a trailing newline`() {
        assertEquals("Bucket,Occurrences\n", formatCsvReport("Bucket,Occurrences", emptyList()))
    }
}
