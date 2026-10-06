@file:Suppress("DEPRECATION")

package io.github.cdsap.gcreport.plugin.fake

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildResult
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.util.Collections
import javax.inject.Inject

/**
 * Test-only stand-in for the Develocity settings plugin, resolved in TestKit builds under the
 * real plugin ID `com.gradle.develocity` (see `META-INF/gradle-plugins/com.gradle.develocity.properties`
 * in the test resources). It registers the `develocity` extension typed as the real
 * [DevelocityConfiguration] (a [Proxy]), so `DevelocityPresence` finds it through the extension
 * schema and the hard cast in `DevelocitySupport` succeeds.
 *
 * Mirrors real Develocity 4.x (and the 3.19.1 jar this repo compiles against): the same instance
 * is also added as a `develocity` extension on the root project from a `gradle.rootProject {}`
 * hook, while `hasPlugin("com.gradle.develocity")` stays false on projects.
 *
 * Build Scan calls are printed instead of published:
 * - `value(name, value)` -> `SCAN-VALUE <name>=<value>`
 * - `tag(tag)`           -> `SCAN-TAG <tag>`
 * - `buildFinished(action)` actions run at build end, so the values/tags they emit print too.
 *
 * Recorded buildFinished actions travel through the configuration cache on the
 * [FakeBuildFinishedHandoff] task (which finalizes every task), are handed to
 * [FakeBuildFinishedService] at execution time, and run when that service is closed at build
 * end — so they also replay when the configuration cache is reused. GCReport's action captures
 * the `develocity` proxy, so the proxies are cache-serializable: their handlers are plain classes
 * and the only back-reference (to the recorded actions) is transient.
 */
abstract class FakeDevelocityPlugin
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) : Plugin<Settings> {
        override fun apply(settings: Settings) {
            println(APPLIED_MARKER)
            val finishedActions = mutableListOf<Any>()
            val develocity = FakeProxies.develocity(objects, finishedActions)
            settings.extensions.add(DevelocityConfiguration::class.java, "develocity", develocity)
            settings.gradle.rootProject { root ->
                root.extensions.add(DevelocityConfiguration::class.java, "develocity", develocity)
            }
            FakeBuildFinishedWiring.install(settings, finishedActions)
        }

        companion object {
            const val APPLIED_MARKER = "FAKE-DEVELOCITY applied"
        }
    }

/**
 * Test-only stand-in for the legacy Gradle Enterprise settings plugin (`com.gradle.enterprise`),
 * registering the `gradleEnterprise` extension typed as the real [GradleEnterpriseExtension].
 * Like real Gradle Enterprise 3.x (checked against 3.12.3), the same instance is also added to the
 * root project from a `gradle.rootProject {}` hook.
 */
abstract class FakeGradleEnterprisePlugin
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) : Plugin<Settings> {
        override fun apply(settings: Settings) {
            println(APPLIED_MARKER)
            val finishedActions = mutableListOf<Any>()
            val buildScan = FakeProxies.buildScan(BuildScanExtension::class.java, objects, finishedActions)
            val gradleEnterprise =
                FakeProxies.create(GradleEnterpriseExtension::class.java, objects, ConfigurationBehaviour(buildScan))
            settings.extensions.add(GradleEnterpriseExtension::class.java, "gradleEnterprise", gradleEnterprise)
            settings.gradle.rootProject { root ->
                root.extensions.add(GradleEnterpriseExtension::class.java, "gradleEnterprise", gradleEnterprise)
            }
            FakeBuildFinishedWiring.install(settings, finishedActions)
        }

        companion object {
            const val APPLIED_MARKER = "FAKE-GRADLE-ENTERPRISE applied"
        }
    }

internal object FakeBuildFinishedWiring {
    // Gradle stops build services in registration-name order; sorting last means GCReport's
    // `gcReportService` has already printed its console report when buildFinished actions run,
    // which keeps the console line order deterministic.
    private const val SERVICE_NAME = "zzzFakeDevelocityBuildFinished"
    private const val HANDOFF_TASK_NAME = "fakeDevelocityBuildFinished"

    fun install(
        settings: Settings,
        finishedActions: List<Any>,
    ) {
        val service =
            settings.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                FakeBuildFinishedService::class.java,
            ) {}

        settings.gradle.projectsEvaluated { gradle ->
            val root = gradle.rootProject
            val handoff =
                root.tasks.register(HANDOFF_TASK_NAME, FakeBuildFinishedHandoff::class.java) { task ->
                    task.usesService(service)
                    task.service.set(service)
                    task.finishedActions = finishedActions
                }
            gradle.allprojects { project ->
                project.tasks.configureEach { task ->
                    if (!(project == root && task.name == HANDOFF_TASK_NAME)) task.finalizedBy(handoff)
                }
            }
        }
    }
}

/**
 * Carries the recorded buildFinished actions through the configuration cache. Build service
 * parameters are isolated via Java serialization, which the plugin's (non-Serializable)
 * action lambdas do not support, whereas task fields are bean-serialized by the cache.
 */
abstract class FakeBuildFinishedHandoff : DefaultTask() {
    @get:Internal
    abstract val service: Property<FakeBuildFinishedService>

    @get:Internal
    var finishedActions: List<Any> = emptyList()

    @TaskAction
    fun handOff() {
        service.get().finishedActions.addAll(finishedActions)
    }
}

abstract class FakeBuildFinishedService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    val finishedActions: MutableList<Any> = Collections.synchronizedList(mutableListOf())

    override fun close() {
        val result = FakeProxies.create(BuildResult::class.java, null, BuildResultBehaviour())
        finishedActions.forEach {
            @Suppress("UNCHECKED_CAST")
            (it as Action<Any>).execute(result)
        }
    }
}

/** Receives the `SCAN-VALUE`/`SCAN-TAG` lines a fake build scan emits. */
fun interface ScanSink {
    fun emit(line: String)
}

class StdoutScanSink : ScanSink {
    override fun emit(line: String) = println(line)
}

/** Per-type method overrides; returning [FakeProxies.UNHANDLED] falls back to defaults. */
interface FakeBehaviour {
    fun invoke(
        proxy: Any,
        method: Method,
        args: Array<Any?>,
    ): Any?
}

class BuildScanBehaviour(
    private val sink: ScanSink,
    @Transient private var finishedActions: MutableList<Any>?,
) : FakeBehaviour {
    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<Any?>,
    ): Any? {
        when (method.name) {
            "value" -> sink.emit("SCAN-VALUE ${args[0]}=${args[1]}")
            "tag" -> sink.emit("SCAN-TAG ${args[0]}")
            "buildFinished" -> checkNotNull(finishedActions) { "buildFinished registered at execution time" } += args[0]!!
            "background" -> {
                @Suppress("UNCHECKED_CAST")
                (args[0] as Action<Any>).execute(proxy)
            }
            else -> return FakeProxies.UNHANDLED
        }
        return null
    }
}

/** `getBuildScan()` / `buildScan(Action)` of a Develocity or Gradle Enterprise extension. */
class ConfigurationBehaviour(
    private val buildScan: Any,
) : FakeBehaviour {
    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<Any?>,
    ): Any? =
        when (method.name) {
            "getBuildScan" -> buildScan
            "buildScan" -> {
                @Suppress("UNCHECKED_CAST")
                (args[0] as Action<Any>).execute(buildScan)
                null
            }
            else -> FakeProxies.UNHANDLED
        }
}

class BuildResultBehaviour : FakeBehaviour {
    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<Any?>,
    ): Any? = if (method.name == "getFailures") emptyList<Throwable>() else FakeProxies.UNHANDLED
}

class DefaultsOnlyBehaviour : FakeBehaviour {
    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<Any?>,
    ): Any? = FakeProxies.UNHANDLED
}

object FakeProxies {
    val UNHANDLED = Any()

    fun develocity(
        objects: ObjectFactory?,
        finishedActions: MutableList<Any>,
        sink: ScanSink = StdoutScanSink(),
    ): DevelocityConfiguration {
        val buildScan = buildScan(BuildScanConfiguration::class.java, objects, finishedActions, sink)
        return create(DevelocityConfiguration::class.java, objects, ConfigurationBehaviour(buildScan))
    }

    fun <T> buildScan(
        type: Class<T>,
        objects: ObjectFactory?,
        finishedActions: MutableList<Any>,
        sink: ScanSink = StdoutScanSink(),
    ): T = create(type, objects, BuildScanBehaviour(sink, finishedActions))

    fun <T> create(
        type: Class<T>,
        objects: ObjectFactory?,
        behaviour: FakeBehaviour,
    ): T =
        type.cast(
            Proxy.newProxyInstance(
                FakeProxies::class.java.classLoader,
                arrayOf(type),
                DefaultsHandler(type, objects, behaviour),
            ),
        )

    /** Answers anything [behaviour] leaves [UNHANDLED] with a sensible default. */
    private class DefaultsHandler(
        private val type: Class<*>,
        @Transient private var objects: ObjectFactory?,
        private val behaviour: FakeBehaviour,
    ) : InvocationHandler {
        @Transient
        private var cache: MutableMap<String, Any?>? = null

        override fun invoke(
            proxy: Any,
            method: Method,
            args: Array<Any?>?,
        ): Any? {
            val arguments = args ?: emptyArray()
            if (method.declaringClass == Any::class.java) {
                return when (method.name) {
                    "equals" -> proxy === arguments[0]
                    "hashCode" -> System.identityHashCode(proxy)
                    else -> "Fake${type.simpleName}"
                }
            }
            val overridden = behaviour.invoke(proxy, method, arguments)
            if (overridden !== UNHANDLED) return overridden

            if (arguments.size == 1 && arguments[0] is Action<*>) {
                // e.g. publishing(Action) configures the object returned by getPublishing().
                val getter =
                    proxy.javaClass.methods.firstOrNull {
                        it.parameterCount == 0 &&
                            it.name == "get" + method.name.replaceFirstChar(Char::uppercaseChar)
                    }
                if (getter != null) {
                    @Suppress("UNCHECKED_CAST")
                    (arguments[0] as Action<Any>).execute(getter.invoke(proxy))
                }
                return null
            }
            if (arguments.isNotEmpty()) return null
            return synchronized(this) {
                val values = cache ?: mutableMapOf<String, Any?>().also { cache = it }
                values.getOrPut(method.name) { defaultFor(method) }
            }
        }

        private fun defaultFor(method: Method): Any? {
            val returnType = method.returnType
            val elementTypes =
                (method.genericReturnType as? ParameterizedType)
                    ?.actualTypeArguments
                    ?.map { (it as? Class<*>) ?: Any::class.java }
                    .orEmpty()
            return when {
                returnType == Void.TYPE -> null
                returnType == java.lang.Boolean.TYPE -> false
                returnType == Property::class.java ->
                    objects?.property(elementTypes.firstOrNull() ?: Any::class.java)
                returnType == ListProperty::class.java ->
                    objects?.listProperty(elementTypes.firstOrNull() ?: Any::class.java)
                returnType == SetProperty::class.java ->
                    objects?.setProperty(elementTypes.firstOrNull() ?: Any::class.java)
                returnType == MapProperty::class.java ->
                    objects?.mapProperty(
                        elementTypes.getOrNull(0) ?: Any::class.java,
                        elementTypes.getOrNull(1) ?: Any::class.java,
                    )
                returnType.isInterface -> create(returnType, objects, DefaultsOnlyBehaviour())
                else -> null
            }
        }
    }
}
