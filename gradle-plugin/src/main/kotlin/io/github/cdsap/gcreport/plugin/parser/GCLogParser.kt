package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry

class GCLogParser {
    private val gcEntryRegex =
        """\[(\d+\.\d+)([a-z]+)]\[.*?\] GC\((\d+)\) (.+?) (\d+)([A-Z])->(\d+)([A-Z])\((\d+)([A-Z])\) (\d+\.\d+)([a-z]+)""".toRegex()
    private val gcLineRegex = """\[(\d+\.\d+)([a-z]+)]\[.*?\] GC\((\d+)\)""".toRegex()
    private val gcDurationRegex =
        """\[(\d+\.\d+)([a-z]+)]\[.*?\] GC\((\d+)\) (.+?) (\d+\.\d+)([a-z]+)""".toRegex()

    fun parse(lines: List<String>): List<GCEntry> {
        val maxGC = mutableListOf<GCEntry>()

        lines.forEach { line ->
            val match = gcEntryRegex.find(line)
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
            .filter { it.value.size > 1 }.forEach { (id, _) ->
                val x =
                    lines.last { line ->
                        gcLineRegex.find(line)?.groupValues?.get(3) == id
                    }

                val match = gcDurationRegex.find(x)
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
