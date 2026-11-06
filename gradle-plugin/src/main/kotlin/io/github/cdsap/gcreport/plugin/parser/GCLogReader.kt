package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.ParsedGCLog
import java.io.File

class GCLogReader(private val file: File) {
    fun parse() = GCLogParser().parse(file.readLines())
}

class GCLogLoader {
    fun load(paths: List<String>): List<ParsedGCLog> =
        paths
            .map(::File)
            .filter(File::exists)
            .map { file -> ParsedGCLog(file, GCLogReader(file).parse()) }
}
