package io.github.architectplatform.core.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Architecture rules for architect-core.
 *
 * Core is the host-agnostic shared runtime layer. It must not depend on
 * framework-specific types (Micronaut, Spring, etc.) so that it can be
 * consumed by any host (Engine, CLI, tests, future hosts).
 *
 * Portable DI annotations (jakarta.inject) are allowed since they are
 * container-agnostic and used by multiple frameworks.
 */
class CoreArchitectureTest {

  companion object {
    private lateinit var classes: JavaClasses

    @JvmStatic
    @BeforeAll
    fun setup() {
      classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .withImportOption(ImportOption.DoNotIncludeJars())
        .importPackages("io.github.architectplatform.core")
    }
  }

  @Test
  fun `core must not depend on Micronaut framework types`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.core..")
      .should().dependOnClassesThat()
      .resideInAPackage("io.micronaut..")
      .because("architect-core is host-agnostic and must not import Micronaut types")
      .check(classes)
  }

  @Test
  fun `core must not depend on Micronaut serde`() {
    noClasses()
      .that().resideInAPackage("io.github.architectplatform.core..")
      .should().dependOnClassesThat()
      .resideInAPackage("io.micronaut.serde..")
      .because("serialization annotations belong in the engine host, not core")
      .check(classes)
  }
}
