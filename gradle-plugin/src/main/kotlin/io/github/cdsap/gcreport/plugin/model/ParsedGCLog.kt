package io.github.cdsap.gcreport.plugin.model

import java.io.File

data class ParsedGCLog(
    val file: File,
    val entries: List<GCEntry>,
)
