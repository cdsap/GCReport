import org.gradle.api.initialization.resolve.RepositoriesMode

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        // Required for com.gradle:develocity-gradle-plugin (implementation dep; not on Maven Central).
        gradlePluginPortal()
    }
}

rootProject.name = "GCReport"
include("gradle-plugin")
