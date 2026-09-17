package io.github.cdsap.gcreport.plugin.histogram

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HistogramTest {
    @Test
    fun `empty gcEntries returns empty histogram`() {
        val histogram = Histogram(Bucket.Sturges)
        val result = histogram.getHistogram(emptyList())
        assertEquals(emptyList<Pair<String, Int>>(), result)
    }

    @Test
    fun `single gcEntry returns single bucket with End`() {
        val gcEntries =
            listOf(
                GCEntry("1.23", "s", "1", "G1", duration = "10", durationUnit = "ms"),
            )
        val histogram = Histogram(Bucket.Sturges)
        val result = histogram.getHistogram(gcEntries)
        assertEquals(listOf("1.23-End" to 1), result)
    }

    @Test
    fun `multiple gcEntries with same timestamp return single bucket`() {
        val gcEntries =
            listOf(
                GCEntry("1.23", "s", "1", "G1", duration = "10", durationUnit = "ms"),
                GCEntry("1.23", "s", "2", "G1", duration = "12", durationUnit = "ms"),
                GCEntry("1.23", "s", "3", "G1", duration = "11", durationUnit = "ms"),
            )
        val histogram = Histogram(Bucket.Sturges)
        val result = histogram.getHistogram(gcEntries)
        assertEquals(listOf("1.23-End" to 3), result)
    }

    @Test
    fun `multiple gcEntries with different timestamps return correct buckets`() {
        val gcEntries =
            listOf(
                GCEntry("1.23", "s", "1", "G1", duration = "10", durationUnit = "ms"),
                GCEntry("2.34", "s", "2", "G1", duration = "12", durationUnit = "ms"),
                GCEntry("3.45", "s", "3", "G1", duration = "11", durationUnit = "ms"),
                GCEntry("4.56", "s", "4", "G1", duration = "13", durationUnit = "ms"),
            )
        val histogram = Histogram(Bucket.Sturges)
        val result = histogram.getHistogram(gcEntries)
        val expected =
            listOf(
                "0.00-1.11" to 0,
                "1.11-2.22" to 1,
                "2.22-3.33" to 1,
                "3.33-4.44" to 1,
                "4.44-End" to 1,
            )
        assertEquals(expected, result)
    }

    @Test
    fun `gcEntries with timestamps on bucket boundaries are grouped correctly`() {
        val gcEntries =
            listOf(
                GCEntry("1.00", "s", "1", "G1", duration = "10", durationUnit = "ms"),
                GCEntry("2.00", "s", "2", "G1", duration = "12", durationUnit = "ms"),
                GCEntry("3.00", "s", "3", "G1", duration = "11", durationUnit = "ms"),
            )
        val histogram = Histogram(Bucket.Sturges)
        val result = histogram.getHistogram(gcEntries)
        val expected =
            listOf(
                "0.00-0.67" to 0,
                "0.67-1.34" to 1,
                "1.34-2.01" to 1,
                "2.01-2.68" to 0,
                "2.68-End" to 1,
            )
        assertEquals(expected, result)
    }

    @Test
    fun `multiple gcEntries with SquareRoot bucket return correct buckets`() {
        val gcEntries =
            listOf(
                GCEntry("1.23", "s", "1", "G1", duration = "10", durationUnit = "ms"),
                GCEntry("2.34", "s", "2", "G1", duration = "12", durationUnit = "ms"),
                GCEntry("3.45", "s", "3", "G1", duration = "11", durationUnit = "ms"),
                GCEntry("4.56", "s", "4", "G1", duration = "13", durationUnit = "ms"),
            )
        val histogram = Histogram(Bucket.SquareRoot) // Use SquareRoot bucket
        val result = histogram.getHistogram(gcEntries)

        // Expected buckets based on SquareRoot calculation
        val expected =
            listOf(
                "0.00-1.66" to 1,
                "1.66-3.32" to 1,
                "3.32-End" to 2,
            )
        assertEquals(expected, result)
    }

    @Test
    fun `gcEntries with timestamps on bucket boundaries with FreedmanDiaconis are grouped correctly`() {
        val gcEntries =
            listOf(
                GCEntry("1.00", "s", "1", "G1", duration = "10", durationUnit = "ms"),
                GCEntry("2.00", "s", "2", "G1", duration = "12", durationUnit = "ms"),
                GCEntry("3.00", "s", "3", "G1", duration = "11", durationUnit = "ms"),
            )
        val histogram = Histogram(Bucket.FreedmanDiaconis) // Use FreedmanDiaconis bucket
        val result = histogram.getHistogram(gcEntries)

        // Expected buckets based on FreedmanDiaconis calculation
        val expected =
            listOf(
                "0.00-2.77" to 2,
                "2.77-End" to 1,
            )
        assertEquals(expected, result)
    }
}
