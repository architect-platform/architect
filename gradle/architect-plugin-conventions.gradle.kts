import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "io.github.architectplatform.plugins"

extensions.configure<JavaPluginExtension> {
  sourceCompatibility = JavaVersion.toVersion("17")
  targetCompatibility = JavaVersion.toVersion("17")
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
  add("implementation", versionCatalog.findLibrary("architect-api-contract").get())
  add("testImplementation", versionCatalog.findLibrary("junit-jupiter-api").get())
  add("testRuntimeOnly", versionCatalog.findLibrary("junit-jupiter-engine").get())
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/architect-platform/architect")
    credentials {
      username =
        System.getenv("GITHUB_USER")
          ?: project.findProperty("githubUser") as String?
          ?: "github-actions"
      password =
        System.getenv("REGISTRY_TOKEN")
          ?: System.getenv("GITHUB_TOKEN")
          ?: project.findProperty("githubToken") as String?
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

plugins.withId("jacoco") {
  extensions.configure<JacocoPluginExtension> {
    toolVersion = versionCatalog.findVersion("jacoco").get().requiredVersion
  }

  tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
      xml.required.set(true)
      html.required.set(true)
    }
  }

  tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
      rule {
        limit {
          minimum = "0.15".toBigDecimal()
        }
      }
    }
  }

  tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
  }
}
