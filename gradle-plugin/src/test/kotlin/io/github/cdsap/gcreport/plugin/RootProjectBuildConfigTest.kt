package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RootProjectBuildConfigTest {
    @Test
    fun `root project declares kotlin jvm plugin without applying it`() {
        val rootBuildGradle = File("../build.gradle.kts").canonicalFile
        val rootBuildGradleContents = rootBuildGradle.readText()

        assertTrue(
            rootBuildGradleContents.contains("alias(libs.plugins.kotlin.jvm) apply false"),
            "root build.gradle.kts must declare kotlin-jvm with apply false so the aggregator is not a code module",
        )
    }

    @Test
    fun `root project has no source directory`() {
        val rootSrc = File("../src").canonicalFile

        assertFalse(
            rootSrc.exists(),
            "root project should remain a pure aggregator without a src/ directory",
        )
    }
}
