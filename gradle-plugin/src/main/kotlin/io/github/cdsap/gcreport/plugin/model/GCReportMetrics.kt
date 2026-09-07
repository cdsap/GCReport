package io.github.cdsap.gcreport.plugin.model

class GCReportMetrics private constructor(
    private val entries: List<GCEntry>,
) {
    fun collectionTypeCounts(): Map<String, Int> =
        entries.groupBy { it.description }.mapValues { (_, groupedEntries) -> groupedEntries.size }

    fun entriesForHistogram(): List<GCEntry> = entries.filter { it.description != CONCURRENT_MARK_CYCLE }

    fun totalCollections(): Int = entriesForHistogram().size

    companion object {
        const val CONCURRENT_MARK_CYCLE = "Concurrent Mark Cycle"

        fun from(entries: List<GCEntry>): GCReportMetrics = GCReportMetrics(entries)
    }
}
