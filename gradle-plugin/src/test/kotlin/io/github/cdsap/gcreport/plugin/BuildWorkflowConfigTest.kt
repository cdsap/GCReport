package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BuildWorkflowConfigTest {
    @Test
    fun `build workflow does not use the misspelled Ktlinkt step name`() {
        val workflow = File("../.github/workflows/build.yaml").canonicalFile
        assertTrue(workflow.isFile, "Build workflow should exist at ${workflow.path}")

        val contents = workflow.readText()
        assertFalse(
            contents.contains("name: Ktlinkt"),
            "Build workflow must not use the misspelled step name Ktlinkt",
        )
        assertTrue(
            contents.contains("name: Build"),
            "Build workflow should run a single Build step covering ktlintCheck and test",
        )
    }
}
