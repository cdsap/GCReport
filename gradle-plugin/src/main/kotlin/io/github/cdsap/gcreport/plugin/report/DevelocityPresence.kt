package io.github.cdsap.gcreport.plugin.report

import org.gradle.api.plugins.ExtensionAware

/** Finds a Develocity extension without referencing Develocity API types. */
internal object DevelocityPresence {
    private const val CONFIGURATION_CLASS =
        "com.gradle.develocity.agent.gradle.DevelocityConfiguration"

    fun findExtension(host: ExtensionAware): Any? {
        val bySchema =
            host.extensions.extensionsSchema.elements
                .firstOrNull { it.publicType.concreteClass.name == CONFIGURATION_CLASS }
                ?.let { host.extensions.findByName(it.name) }
        if (bySchema != null) {
            return bySchema
        }
        return host.extensions.findByName("develocity")
            ?: host.extensions.findByName("gradleEnterprise")
    }
}
