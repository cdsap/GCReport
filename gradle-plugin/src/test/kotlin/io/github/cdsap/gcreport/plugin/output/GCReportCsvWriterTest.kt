package io.github.cdsap.gcreport.plugin.output

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GCReportCsvWriterTest {
    @TempDir
    lateinit var outputDirectory: File

    @Test
    fun `writes collection counts and expected csv filename`() {
        GCReportCsvWriter(outputDirectory, histogramEnabled = false, histogramBucket = Bucket.SquareRoot)
            .write(logFile(), metrics())

        assertEquals(
            "Collection type,Occurrences\n" +
                "Pause Young (Normal),2\n" +
                "Pause Full (System.gc()),1\n",
            File(outputDirectory, "gc.csv").readText(),
        )
        assertFalse(File(outputDirectory, "histogram_gc.csv").exists())
    }

    @Test
    fun `writes histogram output and expected filename when enabled`() {
        GCReportCsvWriter(outputDirectory, histogramEnabled = true, histogramBucket = Bucket.SquareRoot)
            .write(logFile(), metrics())

        assertEquals(
            "Bucket,Occurrences\n" +
                "0.00-1.00,0\n" +
                "1.00-2.00,1\n" +
                "2.00-3.00,1\n" +
                "3.00-End,1\n",
            File(outputDirectory, "histogram_gc.csv").readText(),
        )
    }

    private fun logFile() = File("/logs/gc.log")

    private fun metrics() =
        GCReportMetrics.from(
            listOf(
                gcEntry("1.00", "Pause Young (Normal)"),
                gcEntry("2.00", "Pause Young (Normal)"),
                gcEntry("3.00", "Pause Full (System.gc())"),
            ),
        )

    private fun gcEntry(
        timeStamp: String,
        description: String,
    ) = GCEntry(
        timeStamp = timeStamp,
        timeStampUnit = "s",
        id = timeStamp,
        description = description,
        duration = "10",
        durationUnit = "ms",
    )
}
