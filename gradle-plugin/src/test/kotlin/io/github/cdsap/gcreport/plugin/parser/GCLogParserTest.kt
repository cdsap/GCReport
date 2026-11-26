package io.github.cdsap.gcreport.plugin.parser

import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GCLogParserTest {
    @Test
    fun `parse empty input`() {
        assertEquals(emptyList<GCEntry>(), GCLogParser().parse(emptyList()))
    }

    @Test
    fun `parse in-memory log lines`() {
        val gcEntries =
            GCLogParser().parse(
                listOf(
                    "[1.164s][info   ][gc          ] GC(0) Pause Young (Concurrent Start) (Metadata GC Threshold) " +
                        "194M->24M(4096M) 4.981ms",
                ),
            )

        assertEquals(
            GCEntry(
                timeStamp = "1.164",
                timeStampUnit = "s",
                id = "0",
                description = "Pause Young (Concurrent Start) (Metadata GC Threshold)",
                duration = "4.981",
                durationUnit = "ms",
                memoryBefore = "194",
                memoryBeforeUnit = "M",
                memoryAfter = "24",
                memoryAfterUnit = "M",
                memory = "4096",
                memoryUnit = "M",
            ),
            gcEntries.single(),
        )
    }

    @Test
    fun `consolidate concurrent cycles after ordinary collections in first seen order`() {
        val gcEntries =
            GCLogParser().parse(
                listOf(
                    "[1.000s][info   ][gc          ] GC(1) Pause Remark 25M->25M(4096M) 0.941ms",
                    "[1.001s][info   ][gc          ] GC(2) Pause Remark 30M->29M(4096M) 0.500ms",
                    "[1.001s][info   ][gc          ] GC(3) Pause Young (Normal) 194M->24M(4096M) 4.981ms",
                    "[1.001s][info   ][gc          ] GC(1) Pause Cleanup 26M->26M(4096M) 0.010ms",
                    "[1.001s][info   ][gc          ] GC(2) Pause Cleanup 29M->29M(4096M) 0.020ms",
                    "[1.001s][info   ][gc          ] GC(2) Concurrent Mark Cycle 8.500ms",
                    "[1.002s][info   ][gc          ] GC(1) Concurrent Mark Cycle 10.974ms",
                ),
            )

        assertEquals(
            listOf(
                GCEntry(
                    timeStamp = "1.001",
                    timeStampUnit = "s",
                    id = "3",
                    description = "Pause Young (Normal)",
                    memoryBefore = "194",
                    memoryBeforeUnit = "M",
                    memoryAfter = "24",
                    memoryAfterUnit = "M",
                    memory = "4096",
                    memoryUnit = "M",
                    duration = "4.981",
                    durationUnit = "ms",
                ),
                GCEntry(
                    timeStamp = "1.002",
                    timeStampUnit = "s",
                    id = "1",
                    description = "Concurrent Mark Cycle",
                    duration = "10.974",
                    durationUnit = "ms",
                ),
                GCEntry(
                    timeStamp = "1.001",
                    timeStampUnit = "s",
                    id = "2",
                    description = "Concurrent Mark Cycle",
                    duration = "8.500",
                    durationUnit = "ms",
                ),
            ),
            gcEntries,
        )
    }
}
