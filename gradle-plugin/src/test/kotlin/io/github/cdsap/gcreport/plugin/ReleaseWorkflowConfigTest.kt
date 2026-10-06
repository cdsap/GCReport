package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ReleaseWorkflowConfigTest {
    @Test
    fun `release workflow publishes both plugins from a matching non snapshot tag`() {
        val workflow = File("../.github/workflows/release.yaml").canonicalFile
        assertTrue(workflow.isFile, "Release workflow should exist at ${workflow.path}")

        val contents = workflow.readText()
        assertTrue(contents.contains("types: [published]"))
        assertTrue(contents.contains("ref: \${{ github.event.release.tag_name }}"))
        assertTrue(contents.contains("java-version: '17'"))
        assertTrue(contents.contains("gradle/actions/wrapper-validation@v4"))
        assertTrue(contents.contains("gradle/actions/setup-gradle@v4"))
        assertTrue(contents.contains("RELEASE_TAG: \${{ github.event.release.tag_name }}"))
        assertTrue(contents.contains("release_version=\"\${RELEASE_TAG#v}\""))
        assertTrue(contents.contains("*-SNAPSHOT*"))
        assertTrue(contents.contains("release_version\" != \"\$plugin_version\""))
        assertTrue(contents.contains("./gradlew :gradle-plugin:validatePlugins :gradle-plugin:test"))
        assertTrue(contents.contains("./gradlew :gradle-plugin:publishPlugins"))
        assertTrue(contents.contains("GRADLE_PUBLISH_KEY: \${{ secrets.GRADLE_PUBLISH_KEY }}"))
        assertTrue(contents.contains("GRADLE_PUBLISH_SECRET: \${{ secrets.GRADLE_PUBLISH_SECRET }}"))
        assertTrue(contents.contains("GRADLE_PUBLISH_KEY:?GRADLE_PUBLISH_KEY is required"))
        assertTrue(contents.contains("GRADLE_PUBLISH_SECRET:?GRADLE_PUBLISH_SECRET is required"))
        val pluginBuild = File("../gradle-plugin/build.gradle.kts").canonicalFile.readText()
        assertTrue(pluginBuild.contains("id = \"io.github.cdsap.gcreport\""))
        assertTrue(pluginBuild.contains("id = \"io.github.cdsap.gcreport.project\""))
        assertTrue(contents.contains("steps.publish.outcome"))
        assertTrue(
            contents.indexOf(":gradle-plugin:validatePlugins") <
                contents.indexOf(":gradle-plugin:publishPlugins"),
            "Plugin validation must happen before publication",
        )
        assertTrue(!contents.contains("java-version: 25"), "Release must not use JDK 25")
    }
}
