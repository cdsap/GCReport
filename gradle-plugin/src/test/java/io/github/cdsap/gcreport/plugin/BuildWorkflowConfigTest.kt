package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BuildWorkflowConfigTest {
    @Test
    fun `build workflow uses correctly spelled Ktlint step name`() {
        val workflow = File("../.github/workflows/build.yaml").canonicalFile
        assertTrue(workflow.isFile, "Build workflow should exist at ${workflow.path}")

        val contents = workflow.readText()
        assertFalse(
            contents.contains("name: Ktlinkt"),
            "Build workflow must not use the misspelled step name Ktlinkt",
        )
        assertTrue(
            contents.contains("name: Ktlint"),
            "Build workflow should name the ktlintCheck step Ktlint",
        )
    }
}
