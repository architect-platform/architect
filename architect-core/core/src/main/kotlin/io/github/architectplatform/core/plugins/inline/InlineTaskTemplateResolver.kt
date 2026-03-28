package io.github.architectplatform.core.plugins.inline

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class InlineTaskTemplateResolver {
  fun resolve(
    templates: Map<String, Any>,
    tasks: Map<String, Any>,
  ): Map<String, Any> {
    val resolvedTemplates = mutableMapOf<String, InlineTaskConfig>()
    val visiting = mutableSetOf<String>()

    fun resolveTemplate(name: String): InlineTaskConfig {
      resolvedTemplates[name]?.let { return it }
      if (!visiting.add(name)) {
        throw IllegalArgumentException("Circular inline task template detected involving '$name'")
      }

      val rawTemplate = templates[name]
        ?: throw IllegalArgumentException("Inline task template '$name' is not defined")
      val template = InlineTaskMapper.objectMapper.convertValue(rawTemplate, InlineTaskConfig::class.java)
      val parent =
        template.extends?.takeIf { it.isNotBlank() }?.let { parentName ->
          resolveTemplate(parentName)
        }
      val resolved = merge(parent, template)
      visiting.remove(name)
      resolvedTemplates[name] = resolved
      return resolved
    }

    return tasks.mapValues { (_, rawTask) ->
      val task = InlineTaskMapper.objectMapper.convertValue(rawTask, InlineTaskConfig::class.java)
      val base =
        task.extends?.takeIf { it.isNotBlank() }?.let { templateName ->
          resolveTemplate(templateName)
        }
      InlineTaskMapper.objectMapper.convertValue(
        merge(base, task),
        object : TypeReference<Map<String, Any>>() {},
      )
    }
  }

  private fun merge(
    base: InlineTaskConfig?,
    child: InlineTaskConfig,
  ): InlineTaskConfig {
    if (base == null) return child.copy(extends = null)

    val baseRequires = base.requires
    val childRequires = child.requires
    val mergedRequires =
      when {
        baseRequires == null -> childRequires
        childRequires == null -> baseRequires
        else ->
          InlineTaskRequirements(
            tools = childRequires.tools.ifEmpty { baseRequires.tools },
            minToolVersions = baseRequires.minToolVersions + childRequires.minToolVersions,
            env = childRequires.env.ifEmpty { baseRequires.env },
            platform = childRequires.platform.ifEmpty { baseRequires.platform },
          )
      }

    return InlineTaskConfig(
      description = child.description.ifBlank { base.description },
      run = child.run.ifBlank { base.run },
      extends = null,
      phase = child.phase ?: base.phase,
      depends = child.depends.ifEmpty { base.depends },
      permissions = child.permissions.ifEmpty { base.permissions },
      requires = mergedRequires,
      timeout = child.timeout ?: base.timeout,
      condition = child.condition ?: base.condition,
      onFailure = child.onFailure ?: base.onFailure,
      retryAttempts = child.retryAttempts ?: base.retryAttempts,
    )
  }
}

internal object InlineTaskMapper {
  val objectMapper =
    com.fasterxml.jackson.databind.ObjectMapper()
      .registerKotlinModule()
      .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
