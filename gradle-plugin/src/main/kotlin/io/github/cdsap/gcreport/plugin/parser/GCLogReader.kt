package io.github.cdsap.gcreport.plugin.parser

import java.io.File

class GCLogReader(private val file: File) {
    fun parse() = GCLogParser().parse(file.readLines())
}
