package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.DataInputStream
import java.io.File

class GradlePluginBuildConfigTest {
    @Test
    fun `gradle plugin build declares junit platform launcher on test runtime classpath`() {
        val buildGradle = File("build.gradle.kts").canonicalFile
        val buildGradleContents = buildGradle.readText()

        assertTrue(
            buildGradleContents.contains("testRuntimeOnly(libs.junit.platform.launcher)"),
            "build.gradle.kts must declare junit-platform-launcher on the test runtime classpath",
        )
    }

    @Test
    fun `version catalog declares junit platform launcher`() {
        val versionCatalog = File("../gradle/libs.versions.toml").canonicalFile
        val versionCatalogContents = versionCatalog.readText()

        assertTrue(
            versionCatalogContents.contains("junit-platform-launcher"),
            "gradle/libs.versions.toml must declare junit-platform-launcher",
        )
    }

    @Test
    fun `gradle plugin build pins jvm toolchain to java 11`() {
        val buildGradleContents = File("build.gradle.kts").canonicalFile.readText()

        assertTrue(
            buildGradleContents.contains("jvmToolchain(11)"),
            "build.gradle.kts must pin kotlin jvmToolchain(11) so published bytecode is stable",
        )
    }

    @Test
    fun `compiled plugin classes target java 11 bytecode`() {
        val classFile =
            File("build/classes/kotlin/main/io/github/cdsap/gcreport/plugin/GCReportPlugin.class")
                .canonicalFile

        assertTrue(classFile.isFile, "Expected compiled class at ${classFile.path}")
        assertEquals(
            JAVA_11_MAJOR_VERSION,
            classFileMajorVersion(classFile),
            "Published plugin bytecode must target Java 11 (major version $JAVA_11_MAJOR_VERSION)",
        )
    }

    private fun classFileMajorVersion(classFile: File): Int =
        DataInputStream(classFile.inputStream().buffered()).use { input ->
            val magic = input.readInt()
            assertEquals(CLASS_FILE_MAGIC, magic, "File is not a valid JVM class file: ${classFile.path}")
            input.readUnsignedShort() // minor version
            input.readUnsignedShort() // major version
        }

    private companion object {
        const val CLASS_FILE_MAGIC = -0x35014542 // 0xCAFEBABE
        const val JAVA_11_MAJOR_VERSION = 55
    }
}
