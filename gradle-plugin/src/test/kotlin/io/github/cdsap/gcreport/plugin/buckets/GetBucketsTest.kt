package io.github.cdsap.gcreport.plugin.buckets

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetBucketsTest {
    @Test
    fun getSizeWithSturgesBucket() {
        val gcEntries =
            listOf(
                GCEntry("1", "ms", "1", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("2", "ms", "2", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("3", "ms", "3", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("4", "ms", "4", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("5", "ms", "5", "desc", duration = "1", durationUnit = "ms"),
            )
        val getBuckets = GetBuckets(Bucket.Sturges)
        val size = getBuckets.getSize(gcEntries)
        assertEquals(1.0, size)
    }

    @Test
    fun getSizeWithSquareRootBucket() {
        val gcEntries =
            listOf(
                GCEntry("1", "ms", "1", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("2", "ms", "2", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("3", "ms", "3", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("4", "ms", "4", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("5", "ms", "5", "desc", duration = "1", durationUnit = "ms"),
            )
        val getBuckets = GetBuckets(Bucket.SquareRoot)
        val size = getBuckets.getSize(gcEntries)
        assertEquals(1.33, size)
    }

    @Test
    fun getSizeWithFreedmanDiaconisBucket() {
        val gcEntries =
            listOf(
                GCEntry("1", "ms", "1", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("2", "ms", "2", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("3", "ms", "3", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("4", "ms", "4", "desc", duration = "1", durationUnit = "ms"),
                GCEntry("5", "ms", "5", "desc", duration = "1", durationUnit = "ms"),
            )
        val getBuckets = GetBuckets(Bucket.FreedmanDiaconis)
        val size = getBuckets.getSize(gcEntries)
        assertEquals(2.34, size)
    }

    @Test
    fun getSizeWithEmptyGCEntries() {
        val gcEntries = emptyList<GCEntry>()
        val getBuckets = GetBuckets(Bucket.Sturges)
        val size = getBuckets.getSize(gcEntries)
        assertEquals(0.0, size)
    }

    @Test
    fun getSizeWithSingleGCEntry() {
        val gcEntries = listOf(GCEntry("1", "ms", "1", "desc", duration = "1", durationUnit = "ms"))
        val getBuckets = GetBuckets(Bucket.Sturges)
        val size = getBuckets.getSize(gcEntries)
        assertEquals(0.0, size)
    }
}
