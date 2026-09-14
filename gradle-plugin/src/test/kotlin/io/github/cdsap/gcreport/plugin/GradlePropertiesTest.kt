package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

class GradlePropertiesTest {
    @Test
    fun `project gradle properties enable cache parallel and jvmargs`() {
        val gradleProperties = File("../gradle.properties").canonicalFile

        assertTrue(gradleProperties.isFile, "gradle.properties should exist at ${gradleProperties.path}")

        val properties =
            Properties().apply {
                gradleProperties.inputStream().use { load(it) }
            }

        assertEquals("true", properties.getProperty("org.gradle.caching"))
        assertEquals("true", properties.getProperty("org.gradle.configuration-cache"))
        assertEquals("true", properties.getProperty("org.gradle.parallel"))
        assertEquals(
            "-Xmx2g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8",
            properties.getProperty("org.gradle.jvmargs"),
        )
    }
}
