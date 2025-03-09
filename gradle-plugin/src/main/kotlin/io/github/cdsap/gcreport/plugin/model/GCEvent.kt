package io.github.cdsap.gcreport.plugin.model

data class GCEvent(
    val id: Int,
    val type: String,
    val operation: String? = null,
    val length: String? = null,
    val memoryBefore: String? = null,
    val memoryAfter: String? = null,
    val totalMemory: String? = null,
    val duration: String? = null,
    val timestamp: String,
)
