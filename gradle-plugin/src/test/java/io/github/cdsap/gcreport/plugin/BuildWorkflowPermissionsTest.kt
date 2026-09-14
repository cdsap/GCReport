package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BuildWorkflowPermissionsTest {
    @Test
    fun `build workflow uses least privilege permissions`() {
        val workflow = File("../.github/workflows/build.yaml").canonicalFile
        assertTrue(workflow.isFile, "Build workflow should exist at ${workflow.path}")

        val workflowText = workflow.readText()
        val permissions = permissionsBlock(workflowText)
        assertEquals(
            mapOf(
                "contents" to "read",
                "checks" to "write",
            ),
            permissions,
            "Build job should grant only contents: read and checks: write",
        )

        assertFalse(
            workflowText.contains("pull-requests:"),
            "Build workflow should not request pull-requests permissions",
        )
        assertFalse(
            workflowText.contains("security-events:"),
            "Build workflow should not request security-events permissions",
        )
        assertFalse(
            workflowText.contains("contents: write"),
            "Build workflow should not grant contents: write",
        )
        assertTrue(
            workflowText.contains("mikepenz/action-junit-report"),
            "Build workflow should still publish a JUnit report check",
        )
    }

    private fun permissionsBlock(workflowText: String): Map<String, String> {
        val lines = workflowText.lines()
        val startIndex =
            lines.indexOfFirst { it.trim() == "permissions:" }.also {
                assertTrue(it >= 0, "Build workflow should declare a permissions block")
            }

        val permissionsIndent = lines[startIndex].takeWhile { it == ' ' }.length
        val entries = linkedMapOf<String, String>()
        for (line in lines.drop(startIndex + 1)) {
            val indent = line.takeWhile { it == ' ' }.length
            if (line.isBlank()) {
                continue
            }
            if (indent <= permissionsIndent) {
                break
            }
            val match = Regex("""(\S+):\s*(\S+)""").matchEntire(line.trim())
            assertTrue(match != null, "Unexpected permissions line: $line")
            entries[match!!.groupValues[1]] = match.groupValues[2]
        }
        return entries
    }
}
