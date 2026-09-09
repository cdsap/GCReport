package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class TestSourceLayoutTest {
    @Test
    fun `kotlin test sources live under src test kotlin not java`() {
        val testKotlin = File("src/test/kotlin").canonicalFile
        val testJava = File("src/test/java").canonicalFile

        assertTrue(testKotlin.isDirectory, "Kotlin test sources should live at ${testKotlin.path}")
        assertTrue(
            testKotlin.walkTopDown().any { it.isFile && it.extension == "kt" },
            "src/test/kotlin should contain Kotlin test sources",
        )
        assertFalse(
            testJava.exists(),
            "Kotlin test sources must not live under src/test/java",
        )
    }
}
