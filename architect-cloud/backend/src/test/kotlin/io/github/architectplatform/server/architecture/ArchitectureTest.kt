package io.github.architectplatform.server.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Architecture tests using ArchUnit to enforce Clean/Hexagonal Architecture principles.
 * 
 * These tests ensure:
 * - Domain layer has no dependencies on other layers
 * - Ports (interfaces) define boundaries
 * - Adapters depend on ports, not vice versa
 * - Application services depend only on domain and ports
 * - Clear separation of concerns
 */
class ArchitectureTest {
    
    companion object {
        private lateinit var classes: JavaClasses
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            classes = ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .withImportOption(ImportOption.DoNotIncludeJars())
                .withImportOption { !it.contains("/$") } // Exclude generated classes
                .importPackages("io.github.architectplatform.server")
        }
    }
    
    @Test
    fun `domain layer should not depend on any other layer`() {
        noClasses()
            .that().resideInAPackage("..application.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..application.ports..",
                "..application.services..",
                "..adapters.."
            )
            .check(classes)
    }
    
    @Test
    fun `application services should only depend on domain and ports and infrastructure`() {
        // Services can use infrastructure (Reactor, Micronaut) but not other adapters
        noClasses()
            .that().resideInAPackage("..application.services..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..")
            .check(classes)
    }
    
    @Test
    fun `ports should not depend on adapters`() {
        noClasses()
            .that().resideInAPackage("..application.ports..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..")
            .check(classes)
    }
    
    @Test
    fun `inbound adapters should depend on inbound ports`() {
        classes()
            .that().resideInAPackage("..adapters.inbound..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..application.ports.inbound..",
                "..application.domain..",
                "..adapters.inbound..",
                "java..",
                "kotlin..",
                "io.micronaut..",
                "org.slf4j..",
                "reactor.."
            )
            .check(classes)
    }
    
    @Test
    fun `outbound adapters should depend on outbound ports`() {
        classes()
            .that().resideInAPackage("..adapters.outbound..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..application.ports.outbound..",
                "..application.domain..",
                "..adapters.outbound..",
                "java..",
                "kotlin..",
                "io.micronaut..",
                "jakarta.."
            )
            .check(classes)
    }
    
    @Test
    fun `use cases should be interfaces in ports inbound package`() {
        classes()
            .that().resideInAPackage("..application.ports.inbound..")
            .and().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces()
            .check(classes)
    }
    
    @Test
    fun `ports should be interfaces in ports outbound package`() {
        classes()
            .that().resideInAPackage("..application.ports.outbound..")
            .and().haveSimpleNameEndingWith("Port")
            .should().beInterfaces()
            .check(classes)
    }
    
    @Test
    fun `services should implement use cases`() {
        classes()
            .that().resideInAPackage("..application.services..")
            .and().haveSimpleNameEndingWith("Service")
            .and().haveSimpleNameNotContaining("EventBroadcast")
            .should().implement(io.github.architectplatform.server.application.ports.inbound.ManageEngineUseCase::class.java)
            .orShould().implement(io.github.architectplatform.server.application.ports.inbound.ManageProjectUseCase::class.java)
            .orShould().implement(io.github.architectplatform.server.application.ports.inbound.TrackExecutionUseCase::class.java)
            .orShould().implement(io.github.architectplatform.server.application.ports.inbound.TrackExecutionEventUseCase::class.java)
            .check(classes)
    }
    
    @Test
    fun `adapters should implement ports`() {
        classes()
            .that().resideInAPackage("..adapters.outbound.persistence..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().implement(io.github.architectplatform.server.application.ports.outbound.EngineInstancePort::class.java)
            .orShould().implement(io.github.architectplatform.server.application.ports.outbound.ProjectPort::class.java)
            .orShould().implement(io.github.architectplatform.server.application.ports.outbound.ExecutionPort::class.java)
            .orShould().implement(io.github.architectplatform.server.application.ports.outbound.ExecutionEventPort::class.java)
            .check(classes)
    }
    
    @Test
    fun `layered architecture should be respected`() {
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..application.domain..")
            .layer("Ports").definedBy("..application.ports..")
            .layer("Application").definedBy("..application.services..")
            .layer("Adapters").definedBy("..adapters..")
            
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Ports", "Application", "Adapters")
            .whereLayer("Ports").mayOnlyBeAccessedByLayers("Application", "Adapters")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters")
            
            .check(classes)
    }
    
    @Test
    fun `domain objects should not have framework annotations`() {
        noClasses()
            .that().resideInAPackage("..application.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "io.micronaut.data..",
                "jakarta.persistence..",
                "org.springframework.."
            )
            .check(classes)
    }
}
