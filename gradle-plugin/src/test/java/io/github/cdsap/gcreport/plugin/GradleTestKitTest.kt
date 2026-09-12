package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class GradleTestKitTest {
    @Test
    fun `functional tests drive GradleRunner through GradleTestKit`() {
        val testSources =
            File("src/test/java/io/github/cdsap/gcreport/plugin")
                .canonicalFile
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter {
                    it.name in
                        setOf(
                            "GCReportPluginWithoutDevelocityTest.kt",
                            "GCReportPluginWithDevelocityTest.kt",
                            "GCReportServiceRegistrationTest.kt",
                        )
                }
                .toList()

        assertTrue(testSources.size == 3, "Expected the three TestKit functional test classes")

        testSources.forEach { source ->
            val text = source.readText()
            assertTrue(
                text.contains("GradleTestKit.runner()"),
                "${source.name} must obtain runners via GradleTestKit.runner()",
            )
            assertFalse(
                text.contains("GradleRunner.create()"),
                "${source.name} must not call GradleRunner.create() directly",
            )
        }

        val helper = File("src/test/java/io/github/cdsap/gcreport/plugin/GradleTestKit.kt").canonicalFile
        assertTrue(helper.isFile, "GradleTestKit helper should exist")
        val helperText = helper.readText()
        assertTrue(helperText.contains("withGradleVersion"))
        assertTrue(helperText.contains("testGradleVersion"))
    }
}
