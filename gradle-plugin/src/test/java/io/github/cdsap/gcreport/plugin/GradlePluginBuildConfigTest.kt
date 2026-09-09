package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
    fun `gradle plugin Maven publication uses descriptive artifactId`() {
        val buildGradleContents = File("build.gradle.kts").canonicalFile.readText()

        assertTrue(
            buildGradleContents.contains("""create<MavenPublication>("pluginMaven")"""),
            "build.gradle.kts must configure the pluginMaven publication",
        )
        assertTrue(
            buildGradleContents.contains("""artifactId = "gcreport-gradle-plugin""""),
            "pluginMaven publication must use descriptive artifactId gcreport-gradle-plugin",
        )
    }

    @Test
    fun `generated pluginMaven POM has descriptive artifactId`() {
        val pomFile = File("build/publications/pluginMaven/pom-default.xml").canonicalFile
        assertTrue(
            pomFile.isFile,
            "Expected generated POM at ${pomFile.path}; run generatePomFileForPluginMavenPublication first",
        )

        val primaryArtifactId =
            Regex("""<artifactId>([^<]+)</artifactId>""")
                .find(pomFile.readText())
                ?.groupValues
                ?.get(1)

        assertTrue(
            primaryArtifactId == "gcreport-gradle-plugin",
            "Expected primary artifactId gcreport-gradle-plugin but was $primaryArtifactId",
        )
    }
}
