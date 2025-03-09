package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry
import java.io.File

class GCLogReader(private val file: File) {
    fun parse(): List<GCEntry> {
        val maxGC = mutableListOf<GCEntry>()
        val lines = file.readLines()

        lines.forEach {
            val regex =
                """\[(\d+\.\d+)([a-z]+)]\[.*?\] GC\((\d+)\) (.+?) (\d+)([A-Z])->(\d+)([A-Z])\((\d+)([A-Z])\) (\d+\.\d+)([a-z]+)""".toRegex()

            val match = regex.find(it)

            if (match != null) {
                val groups = match.groupValues
                maxGC.add(
                    GCEntry(
                        timeStamp = groups[1],
                        timeStampUnit = groups[2],
                        id = groups[3],
                        description = groups[4],
                        memoryBefore = groups[5],
                        memoryBeforeUnit = groups[6],
                        memoryAfter = groups[7],
                        memoryAfterUnit = groups[8],
                        memory = groups[9],
                        memoryUnit = groups[10],
                        duration = groups[11],
                        durationUnit = groups[12],
                    ),
                )
            }
        }

        // Once we have all the GC entries found by th regex, we need to find
        // if we have Concurrent Mark Cycle entries. The current regex will include the
        // phases: Pause Remark and Pause Cleanup, something we are not interested.
        val tempMaxGc = mutableListOf<GCEntry>()
        maxGC.groupBy { it.id }
            .filter { it.value.size > 1 }.forEach {
                val id = it.key
                val regex2 = """\[(\d+\.\d+)([a-z]+)]\[.*?\] GC\($id\)""".toRegex()
                val x =
                    lines.last {
                        val a = regex2.find(it)
                        a != null
                    }

                val regex3 = """\[(\d+\.\d+)([a-z]+)]\[.*?\] GC\((\d+)\) (.+?) (\d+\.\d+)([a-z]+)""".toRegex()
                val match = regex3.find(x)
                if (match != null) {
                    val (timestamp, timestampUnit, id, description, duration, durationUnit) = match.destructured
                    tempMaxGc.add(
                        GCEntry(
                            id = id,
                            description = description,
                            timeStamp = timestamp,
                            timeStampUnit = timestampUnit,
                            duration = duration,
                            durationUnit = durationUnit,
                        ),
                    )
                }
            }

        tempMaxGc.forEach { temp ->
            maxGC.removeIf { it.id == temp.id }
            maxGC.add(temp)
        }
        return maxGC
    }
}
