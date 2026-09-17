package io.github.cdsap.gcreport.plugin.model

import com.google.gson.annotations.SerializedName

data class Model(
    val id: String,
    val buildStartTime: Long,
    val buildDuration: Long,
    val gradleVersion: String,
    val pluginVersion: String,
    val rootProjectName: String,
    val requestedTasks: List<String>,
    val hasFailed: Boolean,
    val tags: List<String>,
    val values: List<Value>,
)

data class Response(
    val id: String,
    val availableAt: Long,
    val buildToolType: String,
    val buildToolVersion: String,
    val buildAgentVersion: String,
    val models: Models,
)

data class Models(
    @SerializedName("gradleAttributes")
    val gradleAttributes: GradleAttributes,
)

data class GradleAttributes(
    val model: Model,
)

data class Value(
    val name: String,
    val value: String,
)
