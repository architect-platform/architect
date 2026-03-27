package io.github.architectplatform.cli.plugin

import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.testing.ArchitectPluginContract
import io.github.architectplatform.api.testing.ArchitectPluginTestKit
import io.github.architectplatform.core.plugin.app.IsolatedPluginClassLoader
import io.github.architectplatform.core.plugin.app.SpiPluginLoader
import java.nio.file.Path
import java.util.ServiceConfigurationError
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class PluginJarValidator(
  private val spiPluginLoader: SpiPluginLoader = SpiPluginLoader(),
) {
  fun validate(jarPath: Path): PluginValidationResult {
    val normalizedPath = jarPath.toAbsolutePath().normalize()
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    require(normalizedPath.exists() && normalizedPath.isRegularFile()) {
      "Plugin JAR does not exist: $normalizedPath"
    }
    require(normalizedPath.extension.lowercase() == "jar") {
      "Expected a .jar file: $normalizedPath"
    }

    val spiImplementations = readSpiImplementations(normalizedPath)
    if (spiImplementations.isEmpty()) {
      errors += "Missing SPI descriptor $SPI_RESOURCE or it does not declare any plugin classes."
    }

    val pluginSummaries = if (errors.isEmpty()) {
      loadPlugins(normalizedPath, spiImplementations, errors, warnings)
    } else {
      emptyList()
    }

    if (pluginSummaries.isEmpty() && errors.isEmpty()) {
      errors += "No ArchitectPlugin implementations could be loaded from $normalizedPath."
    }

    return PluginValidationResult(
      jarPath = normalizedPath,
      spiImplementations = spiImplementations,
      plugins = pluginSummaries,
      errors = errors,
      warnings = warnings,
    )
  }

  fun test(jarPath: Path): PluginTestResult {
    val normalizedPath = jarPath.toAbsolutePath().normalize()
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    require(normalizedPath.exists() && normalizedPath.isRegularFile()) {
      "Plugin JAR does not exist: $normalizedPath"
    }
    require(normalizedPath.extension.lowercase() == "jar") {
      "Expected a .jar file: $normalizedPath"
    }

    val spiImplementations = readSpiImplementations(normalizedPath)
    if (spiImplementations.isEmpty()) {
      errors += "Missing SPI descriptor $SPI_RESOURCE or it does not declare any plugin classes."
    }

    val pluginResults = if (errors.isEmpty()) {
      runPluginTests(normalizedPath, spiImplementations, errors, warnings)
    } else {
      emptyList()
    }

    if (pluginResults.isEmpty() && errors.isEmpty()) {
      errors += "No ArchitectPlugin implementations could be loaded from $normalizedPath."
    }

    return PluginTestResult(
      jarPath = normalizedPath,
      spiImplementations = spiImplementations,
      plugins = pluginResults,
      errors = errors,
      warnings = warnings,
    )
  }

  private fun readSpiImplementations(jarPath: Path): List<String> {
    JarFile(jarPath.toFile()).use { jar ->
      val entry = jar.getJarEntry(SPI_RESOURCE) ?: return emptyList()
      return jar.getInputStream(entry)
        .bufferedReader()
        .useLines { lines ->
          lines
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .toList()
        }
    }
  }

  private fun loadPlugins(
    jarPath: Path,
    spiImplementations: List<String>,
    errors: MutableList<String>,
    warnings: MutableList<String>,
  ): List<PluginSummary> {
    val classLoader = IsolatedPluginClassLoader(
      urls = arrayOf(jarPath.toUri().toURL()),
      parent = javaClass.classLoader,
    )

    return classLoader.use { loader ->
      val plugins = try {
        spiPluginLoader.loadFrom(loader)
      } catch (error: ServiceConfigurationError) {
        errors += "Failed to load plugin implementations from SPI: ${error.message}"
        return@use emptyList()
      }

      val discoveredClasses = plugins.map { it.javaClass.name }.toSet()
      spiImplementations
        .filterNot(discoveredClasses::contains)
        .forEach { warnings += "SPI declared $it but it was not discovered via ServiceLoader." }

      plugins.map { plugin ->
        val deserializationError = validateConfigDeserialization(plugin)
        if (deserializationError != null) {
          errors += "Plugin ${plugin.id} failed config deserialization: $deserializationError"
        }

        PluginSummary(
          className = plugin.javaClass.name,
          pluginId = plugin.id,
          contextKey = plugin.contextKey,
          contextClass = plugin.ctxClass.name,
          configDeserializationValid = deserializationError == null,
        )
      }
    }
  }

  private fun validateConfigDeserialization(plugin: ArchitectPlugin<*>): String? {
    return runCatching {
      @Suppress("UNCHECKED_CAST")
      val typedPlugin = plugin as ArchitectPlugin<Any>
      ArchitectPluginTestKit(typedPlugin)
        .configure(emptyMap<String, Any>())
        .tasks()
    }.exceptionOrNull()?.message
  }

  private fun runPluginTests(
    jarPath: Path,
    spiImplementations: List<String>,
    errors: MutableList<String>,
    warnings: MutableList<String>,
  ): List<PluginTestSummary> {
    val classLoader = IsolatedPluginClassLoader(
      urls = arrayOf(jarPath.toUri().toURL()),
      parent = javaClass.classLoader,
    )

    return classLoader.use { loader ->
      val plugins = try {
        spiPluginLoader.loadFrom(loader)
      } catch (error: ServiceConfigurationError) {
        errors += "Failed to load plugin implementations from SPI: ${error.message}"
        return@use emptyList()
      }

      val discoveredClasses = plugins.map { it.javaClass.name }.toSet()
      spiImplementations
        .filterNot(discoveredClasses::contains)
        .forEach { warnings += "SPI declared $it but it was not discovered via ServiceLoader." }

      plugins.map { plugin ->
        val checks = mutableListOf<PluginCheck>()

        val contractFailure = runContractCheck(plugin)
        checks += PluginCheck(
          name = "contract",
          passed = contractFailure == null,
          detail = contractFailure ?: "Architect plugin contract passed",
        )

        val schemaFailure = validateConfigSchema(plugin)
        checks += PluginCheck(
          name = "config-schema",
          passed = schemaFailure == null,
          detail = schemaFailure ?: "configSchema() returned a valid object schema",
        )

        val registrationFailure = validateTaskRegistration(plugin)
        checks += PluginCheck(
          name = "task-registration",
          passed = registrationFailure == null,
          detail = registrationFailure ?: "Plugin registered at least one task",
        )

        PluginTestSummary(
          className = plugin.javaClass.name,
          pluginId = plugin.id,
          checks = checks,
        )
      }
    }
  }

  private fun runContractCheck(plugin: ArchitectPlugin<*>): String? {
    return runCatching {
      @Suppress("UNCHECKED_CAST")
      val typedPlugin = plugin as ArchitectPlugin<Any>
      ArchitectPluginContract { typedPlugin }.verify()
    }.exceptionOrNull()?.message
  }

  private fun validateTaskRegistration(plugin: ArchitectPlugin<*>): String? {
    return runCatching {
      @Suppress("UNCHECKED_CAST")
      val typedPlugin = plugin as ArchitectPlugin<Any>
      val tasks = ArchitectPluginTestKit(typedPlugin)
        .configure(emptyMap<String, Any>())
        .tasks()
      require(tasks.isNotEmpty()) { "Plugin did not register any tasks" }
    }.exceptionOrNull()?.message
  }

  private fun validateConfigSchema(plugin: ArchitectPlugin<*>): String? {
    val schema = plugin.configSchema() ?: return "configSchema() returned null"
    val schemaType = schema["type"] as? String
      ?: return "configSchema() missing required 'type' field"
    if (schemaType != "object") {
      return "configSchema() type must be 'object', got '$schemaType'"
    }
    val properties = schema["properties"]
    if (properties != null && properties !is Map<*, *>) {
      return "configSchema().properties must be a map when provided"
    }
    return null
  }

  data class PluginValidationResult(
    val jarPath: Path,
    val spiImplementations: List<String>,
    val plugins: List<PluginSummary>,
    val errors: List<String>,
    val warnings: List<String>,
  ) {
    val valid: Boolean
      get() = errors.isEmpty() && plugins.isNotEmpty()
  }

  data class PluginSummary(
    val className: String,
    val pluginId: String,
    val contextKey: String,
    val contextClass: String,
    val configDeserializationValid: Boolean,
  )

  data class PluginTestResult(
    val jarPath: Path,
    val spiImplementations: List<String>,
    val plugins: List<PluginTestSummary>,
    val errors: List<String>,
    val warnings: List<String>,
  ) {
    val valid: Boolean
      get() = errors.isEmpty() && plugins.isNotEmpty() && plugins.all { it.passed }
  }

  data class PluginTestSummary(
    val className: String,
    val pluginId: String,
    val checks: List<PluginCheck>,
  ) {
    val passed: Boolean
      get() = checks.all { it.passed }
  }

  data class PluginCheck(
    val name: String,
    val passed: Boolean,
    val detail: String,
  )

  companion object {
    const val SPI_RESOURCE = "META-INF/services/io.github.architectplatform.api.core.plugins.ArchitectPlugin"
  }
}
