plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

group = "io.github.architectplatform.plugins"

version = libs.versions.pluginDefaultArtifact.get()

java { sourceCompatibility = JavaVersion.toVersion("17") }

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

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

dependencies {
  implementation(libs.architect.api.contract)
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
  useJUnitPlatform()
}

jacoco { toolVersion = libs.versions.jacoco.get() }

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
    html.required.set(true)
  }
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = "0.50".toBigDecimal()
      }
    }
  }
}

tasks.check {
  dependsOn(tasks.jacocoTestCoverageVerification)
}
