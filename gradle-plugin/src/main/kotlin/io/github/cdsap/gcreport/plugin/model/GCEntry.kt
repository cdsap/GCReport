package io.github.cdsap.gcreport.plugin.model

data class GCEntry(
    val timeStamp: String,
    val timeStampUnit: String,
    val id: String,
    val description: String,
    val memoryBefore: String? = null,
    val memoryBeforeUnit: String? = null,
    val memoryAfter: String? = null,
    val memoryAfterUnit: String? = null,
    val memory: String? = null,
    val memoryUnit: String? = null,
    val duration: String,
    val durationUnit: String,
)
