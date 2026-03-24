plugins {
  alias(libs.plugins.kotlin.jvm)
  jacoco
  alias(libs.plugins.pitest)
  alias(libs.plugins.jmh)
}

group = "io.github.architectplatform"
version = libs.versions.architectCoreArtifact.get()

apply(from = "../../gradle/architect-kotlin-alignment.gradle.kts")

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
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.dataformat.yaml)
  implementation("org.yaml:snakeyaml")
  implementation(libs.slf4j.api)
  implementation(libs.json.schema.validator)

  compileOnly(libs.jakarta.inject.api)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.archunit.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)

  jmhImplementation(libs.jmh.core)
  jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}

kotlin {
  jvmToolchain(17)
}

java {
  sourceCompatibility = JavaVersion.toVersion("17")
  targetCompatibility = JavaVersion.toVersion("17")
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

jmh {
  warmupIterations.set(1)
  iterations.set(1)
  fork.set(1)
  benchmarkMode.set(listOf("avgt"))
  timeOnIteration.set("200ms")
}

pitest {
  junit5PluginVersion.set("1.2.1")
  targetClasses.set(listOf("io.github.architectplatform.engine.*"))
  targetTests.set(listOf("io.github.architectplatform.engine.*"))
  mutators.set(listOf("DEFAULTS"))
  outputFormats.set(listOf("HTML", "XML"))
  timestampedReports.set(false)
  threads.set(Runtime.getRuntime().availableProcessors())
  timeoutConstInMillis.set(10000)
  excludedClasses.set(listOf("*Test", "*Test\$*"))
}
