package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class GcDevelocityProjectionTest {
    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun pinLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `projects collection counts total and histogram in publication order`() {
        val values =
            GcDevelocityProjection.values(
                log = "/tmp/gradle_gc.log",
                gcEntries =
                    listOf(
                        entry("Pause Young", "1.00"),
                        entry("Pause Young", "1.00"),
                        entry("Concurrent Mark Cycle", "1.00"),
                        entry("Pause Full", "1.00"),
                    ),
                histogramEnabled = true,
                histogramBucket = Bucket.SquareRoot,
            )

        assertEquals(
            listOf(
                "gc-gradle_gc.log-Pause Young" to "2",
                "gc-gradle_gc.log-Concurrent Mark Cycle" to "1",
                "gc-gradle_gc.log-Pause Full" to "1",
                "gc-gradle_gc.log-total-collections" to "3",
                "gc-gradle_gc.log-histogram" to "[\"1.00-End\": \"3\",]",
            ),
            values,
        )
    }

    @Test
    fun `omits zero total collections while retaining collection counts`() {
        val values =
            GcDevelocityProjection.values(
                log = "gc.log",
                gcEntries = listOf(entry("Concurrent Mark Cycle", "1.00")),
                histogramEnabled = false,
                histogramBucket = Bucket.Sturges,
            )

        assertEquals(
            listOf("gc-gc.log-Concurrent Mark Cycle" to "1"),
            values,
        )
    }

    @Test
    fun `omits histogram when disabled`() {
        assertEquals(
            listOf(
                "gc-gc.log-Pause Young" to "1",
                "gc-gc.log-total-collections" to "1",
            ),
            GcDevelocityProjection.values("gc.log", listOf(entry("Pause Young", "1.00")), false, Bucket.Sturges),
        )
    }

    @Test
    fun `empty entries omit counts and total and retain legacy empty histogram text`() {
        assertEquals(emptyList<Pair<String, String>>(), GcDevelocityProjection.values("gc.log", emptyList(), false, Bucket.Sturges))
        assertEquals(
            listOf("gc-gc.log-histogram" to "]"),
            GcDevelocityProjection.values("gc.log", emptyList(), true, Bucket.Sturges),
        )
    }

    @Test
    fun `concurrent mark cycles are counted but excluded from histogram`() {
        assertEquals(
            listOf(
                "gc-gc.log-Concurrent Mark Cycle" to "1",
                "gc-gc.log-histogram" to "]",
            ),
            GcDevelocityProjection.values("gc.log", listOf(entry("Concurrent Mark Cycle", "1.00")), true, Bucket.Sturges),
        )
    }

    @Test
    fun `formats multiple histogram buckets with quotes spaces and trailing comma`() {
        val values =
            GcDevelocityProjection.values(
                "gc.log",
                listOf(entry("Pause Young", "0.00"), entry("Pause Young", "4.00")),
                true,
                Bucket.SquareRoot,
            )

        assertEquals(
            "gc-gc.log-histogram" to "[\"0.00-2.00\": \"1\", \"2.00-4.00\": \"0\", \"4.00-End\": \"1\",]",
            values.last(),
        )
    }

    private fun entry(
        description: String,
        timeStamp: String,
    ) = GCEntry(
        timeStamp = timeStamp,
        timeStampUnit = "s",
        id = "1",
        description = description,
        duration = "10",
        durationUnit = "ms",
    )
}
