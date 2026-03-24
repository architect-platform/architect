package io.github.architectplatform.engine.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Architecture rules for architect-engine.
 *
 * Engine is the Micronaut host layer. It should contain only:
 * - Micronaut factories/adapters that wire core services
 * - REST controllers and HTTP interfaces
 * - Cloud integration adapters
 * - Application entry point
 *
 * Runtime business logic (task execution, caching, plugin loading,
 * config parsing, secret resolution) must live in architect-core.
 */
class EngineArchitectureTest {

  companion object {
    private lateinit var engineClasses: JavaClasses

    @JvmStatic
    @BeforeAll
    fun setup() {
      engineClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("io.github.architectplatform.engine")
    }
  }

  @Test
  fun `engine should not contain TaskExecutor implementation`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.engine..")
      .should().haveSimpleName("TaskExecutor")
      .because("TaskExecutor lives in architect-core; engine uses RuntimeServiceFactory")
      .check(engineClasses)
  }

  @Test
  fun `engine should not contain TaskCache implementation`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.engine..")
      .should().haveSimpleName("TaskCache")
      .because("TaskCache lives in architect-core; engine uses RuntimeServiceFactory")
      .check(engineClasses)
  }

  @Test
  fun `engine should not contain BashCommandExecutor implementation`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.engine..")
      .should().haveSimpleName("BashCommandExecutor")
      .because("BashCommandExecutor lives in architect-core; engine uses RuntimeServiceFactory")
      .check(engineClasses)
  }

  @Test
  fun `engine should not contain ProjectService implementation`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.engine..")
      .should().haveSimpleName("ProjectService")
      .because("ProjectService lives in architect-core; engine uses ProjectServiceFactory")
      .check(engineClasses)
  }

  @Test
  fun `engine should not contain ConfigLoader implementation`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.engine..")
      .should().haveSimpleName("ConfigLoader")
      .because("ConfigLoader lives in architect-core; engine resolves it from core")
      .check(engineClasses)
  }

  @Test
  fun `engine should not contain SecretResolver interface`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.engine..")
      .should().haveSimpleName("SecretResolver")
      .because("SecretResolver lives in architect-core; engine uses SecretResolverFactory")
      .check(engineClasses)
  }
}
