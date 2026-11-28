package io.github.cdsap.gcreport.plugin.output

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HistogramValueFormatterTest {
    @Test
    fun `empty histogram retains legacy closing bracket`() {
        assertEquals("]", HistogramValueFormatter.format(emptyList()))
    }

    @Test
    fun `single bucket retains quotes spaces and trailing comma`() {
        assertEquals(
            "[\"1.00-End\": \"3\",]",
            HistogramValueFormatter.format(listOf("1.00-End" to 3)),
        )
    }

    @Test
    fun `multiple buckets retain input order zero counts and punctuation`() {
        assertEquals(
            "[\"4.00-End\": \"1\", \"0.00-2.00\": \"2\", \"2.00-4.00\": \"0\",]",
            HistogramValueFormatter.format(
                listOf(
                    "4.00-End" to 1,
                    "0.00-2.00" to 2,
                    "2.00-4.00" to 0,
                ),
            ),
        )
    }
}
