package io.github.cdsap.gcreport.plugin.output

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CollectionTypeReportTest {
    private val counts =
        listOf(
            "Pause Young (Normal)" to 12,
            "Pause Full (System.gc())" to 1,
        )

    @Test
    fun `renders exact console layout in collection order`() {
        assertEquals(
            """
            ┌────────────────────────────────────────┐
            │ GC Log: gc.log                         │
            ├──────────────────────────┬─────────────┤
            │ Collection type          │ Occurrences │
            ├──────────────────────────┼─────────────┤
            │ Pause Young (Normal)     │          12 │
            ├──────────────────────────┼─────────────┤
            │ Pause Full (System.gc()) │           1 │
            └──────────────────────────┴─────────────┘
            """.trimIndent(),
            CollectionTypeReport.consoleTable("gc.log", counts),
        )
    }

    @Test
    fun `renders exact csv in collection order with trailing newline`() {
        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),12\n" +
                "Pause Full (System.gc()),1\n",
            CollectionTypeReport.csv(counts),
        )
    }

    @Test
    fun `preserves csv labels without escaping`() {
        assertEquals(
            "Collection type,Occurrences\nlabel,with comma,0\n",
            CollectionTypeReport.csv(listOf("label,with comma" to 0)),
        )
    }

    @Test
    fun `renders empty csv with header and trailing newline`() {
        assertEquals("Collection type,Occurrences\n", CollectionTypeReport.csv(emptyList()))
    }
}
