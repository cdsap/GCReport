package io.github.cdsap.gcreport.plugin.buckets

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry

class GetBuckets(private val bucket: Bucket) {
    fun getSize(gcEntries: List<GCEntry>): Double {
        if (gcEntries.isEmpty()) {
            return 0.0
        }
        val max = gcEntries.map { it.timeStamp.toDouble() }
        val range = max.maxOrNull()!! - max.minOrNull()!!
        val n = max.size

        val size =
            when (bucket) {
                Bucket.Sturges -> {
                    val sturgesBucketCount = Math.ceil(Math.log(n.toDouble()) / Math.log(2.0) + 1).toInt()
                    range / sturgesBucketCount
                }

                Bucket.SquareRoot -> {
                    val sqrtBucketCount = Math.ceil(Math.sqrt(n.toDouble())).toInt()
                    range / sqrtBucketCount
                }

                Bucket.FreedmanDiaconis -> {
                    val sorted = max.sorted()
                    val q1 = sorted[(n * 0.25).toInt()]
                    val q3 = sorted[(n * 0.75).toInt()]
                    val iqr = q3 - q1
                    2 * iqr / Math.cbrt(n.toDouble())
                }
            }

        return String.format("%.2f", size).toDouble()
    }
}
