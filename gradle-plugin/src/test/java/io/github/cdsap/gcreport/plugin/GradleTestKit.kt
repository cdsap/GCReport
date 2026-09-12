package io.github.cdsap.gcreport.plugin

import org.gradle.testkit.runner.GradleRunner

internal object GradleTestKit {
    fun runner(): GradleRunner {
        val runner = GradleRunner.create()
        val version =
            System.getProperty("testGradleVersion")
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("TEST_GRADLE_VERSION")?.takeIf { it.isNotBlank() }
        if (version != null) {
            runner.withGradleVersion(version)
        }
        return runner
    }
}
