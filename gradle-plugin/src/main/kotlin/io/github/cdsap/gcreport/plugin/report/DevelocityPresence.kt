package io.github.cdsap.gcreport.plugin.report

import org.gradle.api.Project

/** Finds a Develocity extension without referencing Develocity API types. */
internal object DevelocityPresence {
    private const val CONFIGURATION_CLASS =
        "com.gradle.develocity.agent.gradle.DevelocityConfiguration"

    fun findExtension(project: Project): Any? {
        val bySchema =
            project.extensions.extensionsSchema.elements
                .firstOrNull { it.publicType.concreteClass.name == CONFIGURATION_CLASS }
                ?.let { project.extensions.findByName(it.name) }
        if (bySchema != null) {
            return bySchema
        }
        return project.extensions.findByName("develocity")
            ?: project.extensions.findByName("gradleEnterprise")
    }
}
