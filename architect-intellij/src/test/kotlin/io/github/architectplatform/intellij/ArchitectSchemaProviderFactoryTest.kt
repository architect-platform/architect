package io.github.architectplatform.intellij

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ArchitectSchemaProviderFactoryTest : BasePlatformTestCase() {
  fun testProviderIsAvailableForArchitectConfigFiles() {
    val provider = ArchitectSchemaProviderFactory().getProviders(project).single()

    val architectFile = myFixture.configureByText("architect.yml", "project:\n  name: demo")
    val architectYamlFile = myFixture.configureByText("architect.yaml", "project:\n  name: demo")
    val otherFile = myFixture.configureByText("application.yml", "micronaut:\n  application: demo")

    assertTrue(provider.isAvailable(architectFile.virtualFile))
    assertTrue(provider.isAvailable(architectYamlFile.virtualFile))
    assertFalse(provider.isAvailable(otherFile.virtualFile))
    assertNotNull(provider.schemaFile)
  }
}
