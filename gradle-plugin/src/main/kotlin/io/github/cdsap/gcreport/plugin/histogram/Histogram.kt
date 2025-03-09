package io.github.cdsap.gcreport.plugin.histogram

import io.github.cdsap.gcreport.plugin.buckets.GetBuckets
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry

class Histogram(private val bucket: Bucket) {
    fun getHistogram(gcEntries: List<GCEntry>): List<Pair<String, Int>> {
        if (gcEntries.isEmpty()) {
            return emptyList()
        }
        val timestamps = gcEntries.map { it.timeStamp.toDouble() }
        val bucketsSize = GetBuckets(bucket).getSize(gcEntries)

        // unlikely event to handle the case when bucketsSize is 0 (all timestamps are the same)
        if (bucketsSize == 0.0) {
            val formattedTimestamp = String.format("%.2f", timestamps.first())
            return listOf("$formattedTimestamp-End" to gcEntries.size)
        }

        val buckets = (0..(timestamps.maxOrNull()!! / bucketsSize).toInt()).map { it * bucketsSize }

        val histogram =
            buckets.mapIndexed { index, bucket ->
                val end = buckets.getOrNull(index + 1) ?: Double.POSITIVE_INFINITY
                val count = timestamps.count { it >= bucket && it < end } // Use end directly

                val formattedBucket = String.format("%.2f", bucket)
                val formattedEnd = if (end.isFinite()) String.format("%.2f", end) else "End"

                "$formattedBucket-$formattedEnd" to count
            }
        return histogram
    }
}
