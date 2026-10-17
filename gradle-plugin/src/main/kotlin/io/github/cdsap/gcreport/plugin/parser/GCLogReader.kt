package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry
import java.io.File

class GCLogReader(private val file: File) {
    fun parse() = GCLogParser().parse(file.readLines())
}

data class ParsedGCLog(
    val file: File,
    val entries: List<GCEntry>,
)

class GCLogLoader {
    fun load(paths: List<String>): List<ParsedGCLog> =
        paths
            .map(::File)
            .filter(File::exists)
            .map { file -> ParsedGCLog(file, GCLogReader(file).parse()) }
}
