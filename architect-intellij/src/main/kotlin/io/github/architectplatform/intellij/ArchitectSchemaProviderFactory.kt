package io.github.architectplatform.intellij

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.intellij.openapi.project.Project

class ArchitectSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
    return listOf(ArchitectSchemaProvider())
  }
}

private class ArchitectSchemaProvider : JsonSchemaFileProvider {
  override fun isAvailable(file: VirtualFile): Boolean {
    return file.name == "architect.yml" || file.name == "architect.yaml"
  }

  override fun getName(): String = "Architect Configuration"

  override fun getSchemaFile(): VirtualFile? {
    return JsonSchemaProviderFactory.getResourceFile(
      ArchitectSchemaProvider::class.java,
      "/schema/architect.yml.json"
    )
  }

  override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
}
