val kotlinVersion = project.properties["kotlinVersion"] as String? ?: "1.9.24"

version = "1.1.0"

group = "io.github.architectplatform"

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.25"
  id("org.jetbrains.kotlin.kapt") version "1.9.25"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("io.micronaut.application") version "4.6.1"
  id("io.micronaut.aot") version "4.6.1"
}

repositories { mavenCentral() }

dependencies {
  kapt("info.picocli:picocli-codegen")
  kapt("io.micronaut.serde:micronaut-serde-processor")
  implementation("info.picocli:picocli")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("io.micronaut.picocli:micronaut-picocli")
  implementation("io.micronaut.serde:micronaut-serde-jackson")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.micronaut.reactor:micronaut-reactor")
  implementation("io.micronaut:micronaut-http-client")
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("org.yaml:snakeyaml")
}

application { mainClass = "io.github.architectplatform.cli.ArchitectLauncher" }

graalvmNative.toolchainDetection.set(false)

micronaut {
  testRuntime("junit5")
  processing {
    incremental(true)
    annotations("io.github.architectplatform.cli.*")
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

// Architect

java { sourceCompatibility = JavaVersion.toVersion("17") }

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

// Enforce Kotlin version coherence
configurations
    .matching { it.name != "detekt" }
    .all {
      resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
          useVersion(kotlinVersion)
          because(
              "All Kotlin modules should use the same version, and compiler uses $kotlinVersion")
        }
      }
    }

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("1.9.25")
    }
    if (requested.group == "org.jetbrains.kotlinx") {
      useVersion("1.8.1") // coroutines version compatible with Kotlin 1.9.x
    }
  }
}
