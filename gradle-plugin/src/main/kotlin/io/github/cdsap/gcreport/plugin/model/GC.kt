package io.github.cdsap.gcreport.plugin.model

data class GC(
    val type: GCType,
    val logFileType: GCLogFile,
    val events: List<GCEvent>,
)
