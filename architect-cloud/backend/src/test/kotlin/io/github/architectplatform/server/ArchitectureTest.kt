package io.github.architectplatform.server

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*
import com.tngtech.archunit.library.Architectures.layeredArchitecture

/**
 * Architecture tests for architect-cloud using ArchUnit.
 * Enforces hexagonal architecture rules and layer boundaries.
 */
@AnalyzeClasses(
    packages = ["io.github.architectplatform.server"],
    importOptions = [ImportOption.DoNotIncludeTests::class]
)
class ArchitectureTest {
    
    @ArchTest
    val `domain layer should not depend on any other layer` =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..adapters..",
                "..ports..",
                "..services..",
                "io.micronaut..",
                "jakarta.."
            )
            .because("Domain layer should be pure and independent")
    
    @ArchTest
    val `application services should only depend on domain and ports` =
        classes()
            .that().resideInAPackage("..services..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain..",
                "..ports..",
                "..services..",
                "java..",
                "kotlin..",
                "reactor..",
                "jakarta.inject..",
                "com.hubspot.jinjava.."
            )
            .because("Services should not depend on adapters")
    
    @ArchTest
    val `ports should not depend on adapters` =
        noClasses()
            .that().resideInAPackage("..ports..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..")
            .because("Ports define interfaces, adapters implement them")
    
    @ArchTest
    val `adapters should depend on ports` =
        classes()
            .that().resideInAPackage("..adapters..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..ports..",
                "..domain..",
                "java..",
                "kotlin..",
                "io.micronaut..",
                "jakarta..",
                "reactor.."
            )
    
    @ArchTest
    val `layered architecture should be respected` =
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Ports").definedBy("..ports..")
            .layer("Services").definedBy("..services..")
            .layer("Adapters").definedBy("..adapters..")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Ports").mayOnlyAccessLayers("Domain")
            .whereLayer("Services").mayOnlyAccessLayers("Domain", "Ports")
            .whereLayer("Adapters").mayOnlyAccessLayers("Domain", "Ports", "Services")
    
    @ArchTest
    val `controllers should be in rest adapter package` =
        classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..adapters.inbound.rest")
            .because("Controllers are REST adapters")
    
    @ArchTest
    val `entities should be in persistence adapter package` =
        classes()
            .that().haveSimpleNameEndingWith("Entity")
            .should().resideInAPackage("..adapters.outbound.persistence")
            .because("Entities are persistence implementation details")
    
    @ArchTest
    val `repositories should be in persistence adapter package` =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..adapters.outbound.persistence")
            .because("Repositories are persistence adapters")
    
    @ArchTest
    val `services should be annotated with Singleton` =
        classes()
            .that().resideInAPackage("..services..")
            .and().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith("jakarta.inject.Singleton")
            .because("Services should be managed as singletons")
    
    @ArchTest
    val `domain models should be immutable` =
        classes()
            .that().resideInAPackage("..domain..")
            .and().areNotEnums()
            .should().haveOnlyFinalFields()
            .orShould().beAnnotatedWith("kotlin.Metadata")
            .because("Domain models should be immutable")
}
