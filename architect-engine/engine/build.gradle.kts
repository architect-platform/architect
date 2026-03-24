plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.ksp)
  alias(libs.plugins.shadow)
  alias(libs.plugins.micronaut.application)
  alias(libs.plugins.micronaut.aot)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.detekt)
  jacoco
}

version = libs.versions.architectEngineArtifact.get()

group = "io.github.architectplatform"

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
  implementation(libs.architect.core.module)
  implementation(libs.architect.api.contract)
  ksp(libs.micronaut.http.validation)
  ksp(libs.micronaut.serde.processor)
  implementation(libs.micronaut.kotlin.runtime)
  implementation(libs.micronaut.serde.jackson)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.jackson.module.kotlin)
  compileOnly(libs.micronaut.http.client)
  implementation(libs.micronaut.runtime)
  runtimeOnly(libs.micronaut.http.client)
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly(libs.jackson.module.kotlin)
  runtimeOnly("org.yaml:snakeyaml")
  testImplementation(libs.micronaut.http.client)
  testImplementation(libs.architect.cli.module)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.mockito.core)
}

application { mainClass.set("io.github.architectplatform.engine.ApplicationKt") }

kotlin { jvmToolchain(17) }

java {
  sourceCompatibility = JavaVersion.toVersion("17")
  targetCompatibility = JavaVersion.toVersion("17")
}

graalvmNative.toolchainDetection.set(false)

micronaut {
  runtime("netty")
  testRuntime("junit5")
  processing {
    incremental(true)
    annotations("io.github.architectplatform.engine.*")
  }
  aot {
    // Please review carefully the optimizations enabled below
    // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
    optimizeServiceLoading.set(false)
    convertYamlToJava.set(false)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
    deduceEnvironment.set(true)
    optimizeNetty.set(true)
  }
}

jacoco { toolVersion = libs.versions.jacoco.get() }

ktlint {
  version.set("1.0.1")
  verbose.set(true)
  android.set(false)
  ignoreFailures.set(true) // Report but do not break the build during adoption phase
}

detekt {
  config.setFrom(rootProject.file("../../detekt.yml"))
  buildUponDefaultConfig = true
  ignoreFailures = true // Report but do not block the build during adoption phase
  // Detect baseline file next to build.gradle.kts, created with `./gradlew detektBaseline`
  baseline = file("detekt-baseline.xml")
}

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
