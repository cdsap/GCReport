package io.github.cdsap.gcreport.plugin.model

const val CONCURRENT_MARK_CYCLE = "Concurrent Mark Cycle"

fun List<GCEntry>.collectionTypeCounts(): Map<String, Int> =
    groupBy { it.description }.mapValues { (_, groupedEntries) -> groupedEntries.size }

fun List<GCEntry>.entriesForHistogram(): List<GCEntry> = filter { it.description != CONCURRENT_MARK_CYCLE }
