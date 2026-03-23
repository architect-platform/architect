package io.github.architectplatform.engine.core.schema

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ArchitectSchemaGeneratorTest {

  private val mapper = ObjectMapper()

  @Test
  fun `schema has correct meta fields`() {
    val schema = ArchitectSchemaGenerator.generate()
    assertEquals("http://json-schema.org/draft-07/schema#", schema.get("\$schema").asText())
    assertEquals("https://architect.dev/schema/architect.yml.json", schema.get("\$id").asText())
    assertEquals("object", schema.get("type").asText())
  }

  @Test
  fun `project is required`() {
    val schema = ArchitectSchemaGenerator.generate()
    val required = schema.get("required")
    assertNotNull(required)
    assertTrue(required.any { it.asText() == "project" })
  }

  @Test
  fun `project name is required`() {
    val schema = ArchitectSchemaGenerator.generate()
    val project = schema.get("properties").get("project")
    val required = project.get("required")
    assertNotNull(required)
    assertTrue(required.any { it.asText() == "name" })
  }

  @Test
  fun `plugins schema is an array of plugin configs`() {
    val schema = ArchitectSchemaGenerator.generate()
    val plugins = schema.get("properties").get("plugins")
    assertEquals("array", plugins.get("type").asText())
    assertNotNull(plugins.get("items").get("\$ref"))
  }

  @Test
  fun `tasks schema uses additionalProperties for dynamic keys`() {
    val schema = ArchitectSchemaGenerator.generate()
    val tasks = schema.get("properties").get("tasks")
    assertEquals("object", tasks.get("type").asText())
    assertNotNull(tasks.get("additionalProperties").get("\$ref"))
  }

  @Test
  fun `plugin config definition includes name as required`() {
    val schema = ArchitectSchemaGenerator.generate()
    val pluginDef = schema.get("definitions").get("pluginConfig")
    val required = pluginDef.get("required")
    assertNotNull(required)
    assertTrue(required.any { it.asText() == "name" })
  }

  @Test
  fun `plugin config definition includes process command property`() {
    val schema = ArchitectSchemaGenerator.generate()
    val pluginDef = schema.get("definitions").get("pluginConfig")
    val props = pluginDef.get("properties")

    assertNotNull(props.get("command"))
    assertEquals("string", props.get("command").get("type").asText())
    assertNotNull(props.get("package"))
    assertEquals("string", props.get("package").get("type").asText())
    assertNotNull(props.get("verify-signature"))
    assertEquals("boolean", props.get("verify-signature").get("type").asText())
    assertNotNull(props.get("trusted-keys"))
    assertEquals("array", props.get("trusted-keys").get("type").asText())
  }

  @Test
  fun `plugin config definition requires command when type is process`() {
    val schema = ArchitectSchemaGenerator.generate()
    val allOf = schema.get("definitions").get("pluginConfig").get("allOf")

    assertNotNull(allOf)
    assertTrue(allOf.any {
      val typeConst = it.get("if")?.get("properties")?.get("type")?.get("const")?.asText()
      val requiredFields = it.get("then")?.get("required")
      typeConst == "process" && requiredFields != null && requiredFields.any { field -> field.asText() == "command" }
    })
    assertFalse(allOf.isEmpty)
  }

  @Test
  fun `plugin config definition requires package when type is npm`() {
    val schema = ArchitectSchemaGenerator.generate()
    val allOf = schema.get("definitions").get("pluginConfig").get("allOf")

    assertNotNull(allOf)
    assertTrue(allOf.any {
      val typeConst = it.get("if")?.get("properties")?.get("type")?.get("const")?.asText()
      val requiredFields = it.get("then")?.get("required")
      typeConst == "npm" && requiredFields != null && requiredFields.any { field -> field.asText() == "package" }
    })
  }

  @Test
  fun `inline task definition has run and phase properties`() {
    val schema = ArchitectSchemaGenerator.generate()
    val taskDef = schema.get("definitions").get("inlineTask")
    val props = taskDef.get("properties")
    assertNotNull(props.get("run"))
    assertNotNull(props.get("phase"))
    assertNotNull(props.get("depends"))
    assertNotNull(props.get("permissions"))
    assertNotNull(props.get("description"))
  }

  @Test
  fun `phase enum includes all workflow phases`() {
    val schema = ArchitectSchemaGenerator.generate()
    val taskDef = schema.get("definitions").get("inlineTask")
    val phaseEnum = taskDef.get("properties").get("phase").get("enum")
    val values = phaseEnum.map { it.asText() }
    // CoreWorkflow
    assertTrue("INIT" in values)
    assertTrue("BUILD" in values)
    assertTrue("TEST" in values)
    assertTrue("PUBLISH" in values)
    // CodeWorkflow
    assertTrue("CODE-build" in values)
    // HooksWorkflow
    assertTrue("pre-commit" in values)
  }

  @Test
  fun `plugin type enum includes npm`() {
    val schema = ArchitectSchemaGenerator.generate()
    val pluginTypeEnum =
      schema.get("definitions").get("pluginConfig").get("properties").get("type").get("enum")
    val values = pluginTypeEnum.map { it.asText() }

    assertTrue("npm" in values)
  }

  @Test
  fun `generateJson produces valid JSON`() {
    val json = ArchitectSchemaGenerator.generateJson()
    val parsed = mapper.readTree(json)
    assertNotNull(parsed)
    assertEquals("Architect Configuration", parsed.get("title").asText())
  }
}
