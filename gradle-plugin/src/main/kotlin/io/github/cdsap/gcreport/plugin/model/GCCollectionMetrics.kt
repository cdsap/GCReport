package io.github.cdsap.gcreport.plugin.model

class GCCollectionMetrics(
    private val entries: List<GCEntry>,
) {
    fun countsByDescription(): Map<String, Int> =
        entries.groupBy { it.description }.mapValues { (_, groupedEntries) -> groupedEntries.size }

    fun entriesForHistogram(): List<GCEntry> = entries.filter { it.description != CONCURRENT_MARK_CYCLE }

    fun totalCollections(): Int = entriesForHistogram().size

    companion object {
        const val CONCURRENT_MARK_CYCLE = "Concurrent Mark Cycle"
    }
}
