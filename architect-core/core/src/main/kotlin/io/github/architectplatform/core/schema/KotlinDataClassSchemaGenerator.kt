package io.github.architectplatform.core.schema

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

/**
 * Generates a JSON Schema Map from a Kotlin data class using reflection.
 *
 * Supports:
 * - Primitives: String, Boolean, Int, Long, Double, Float
 * - Nullable types → no "required" entry
 * - Default values via primary constructor parameter defaults
 * - List<T> → "type": "array"
 * - Map<String, V> → "type": "object", "additionalProperties": schema(V)
 * - Nested data classes → recursive schema
 */
object KotlinDataClassSchemaGenerator {

  fun generate(clazz: Class<*>): Map<String, Any> {
    return generateForKClass(clazz.kotlin)
  }

  private fun generateForKClass(kClass: KClass<*>): Map<String, Any> {
    val constructor = kClass.primaryConstructor
      ?: return mapOf("type" to "object", "additionalProperties" to true)

    val properties = mutableMapOf<String, Any>()
    val requiredProps = mutableListOf<String>()

    for (param in constructor.parameters) {
      val name = param.name ?: continue
      val schema = typeToSchema(param.type)
      val propSchema = schema.toMutableMap()

      if (param.isOptional) {
        val defaultValue = getDefaultValue(param)
        if (defaultValue != null) {
          propSchema["default"] = defaultValue
        }
      } else if (!param.type.isMarkedNullable) {
        requiredProps += name
      }

      properties[name] = propSchema
    }

    val result = mutableMapOf<String, Any>(
      "type" to "object",
      "additionalProperties" to false,
      "properties" to properties,
    )
    if (requiredProps.isNotEmpty()) {
      result["required"] = requiredProps
    }
    return result
  }

  private fun typeToSchema(kType: KType): Map<String, Any> {
    val classifier = kType.classifier as? KClass<*> ?: return mapOf("type" to "object")

    return when (classifier) {
      String::class -> mapOf("type" to "string")
      Boolean::class -> mapOf("type" to "boolean")
      Int::class, Long::class, Short::class, Byte::class -> mapOf("type" to "integer")
      Double::class, Float::class -> mapOf("type" to "number")
      List::class, MutableList::class -> {
        val elementType = kType.arguments.firstOrNull()?.type
        val itemSchema: Map<String, Any> = if (elementType != null) typeToSchema(elementType) else mapOf("type" to "string")
        mapOf("type" to "array", "items" to itemSchema)
      }
      Map::class, MutableMap::class -> {
        val valueType = kType.arguments.getOrNull(1)?.type
        val valueSchema: Map<String, Any> = if (valueType != null) typeToSchema(valueType) else mapOf("type" to "string")
        mapOf("type" to "object", "additionalProperties" to valueSchema)
      }
      Set::class, MutableSet::class -> {
        val elementType = kType.arguments.firstOrNull()?.type
        val itemSchema: Map<String, Any> = if (elementType != null) typeToSchema(elementType) else mapOf("type" to "string")
        mapOf("type" to "array", "uniqueItems" to true, "items" to itemSchema)
      }
      else -> {
        if (classifier.isData) {
          generateForKClass(classifier)
        } else if (classifier.java.isEnum) {
          val enumValues = classifier.java.enumConstants?.map { it.toString() } ?: emptyList<String>()
          mapOf("type" to "string", "enum" to enumValues)
        } else {
          mapOf("type" to "object", "additionalProperties" to true)
        }
      }
    }
  }

  private fun getDefaultValue(param: KParameter): Any? {
    if (!param.isOptional) return null
    return when (param.type.classifier) {
      Boolean::class -> false
      Int::class -> 0
      Long::class -> 0L
      Double::class -> 0.0
      String::class -> null
      List::class, MutableList::class, Set::class, MutableSet::class -> emptyList<Any>()
      Map::class, MutableMap::class -> emptyMap<String, Any>()
      else -> null
    }
  }
}
