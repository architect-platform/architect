plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.25"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
  id("com.google.devtools.ksp") version "1.9.25-1.0.20"
  id("com.gradleup.shadow") version "8.3.5"
  id("io.micronaut.application") version "4.6.1"
  id("io.micronaut.aot") version "4.6.1"
}

version = "1.0.0"

group = "io.github.architectplatform"

val kotlinVersion = project.properties.get("kotlinVersion")

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  ksp("io.micronaut:micronaut-http-validation")
  ksp("io.micronaut.serde:micronaut-serde-processor")
  ksp("io.micronaut.data:micronaut-data-processor")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("io.micronaut.serde:micronaut-serde-jackson")
  implementation("io.micronaut.data:micronaut-data-jdbc")
  implementation("io.micronaut.sql:micronaut-jdbc-hikari")
  implementation("io.micronaut.reactor:micronaut-reactor")
  implementation("io.micronaut:micronaut-websocket")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.micronaut:micronaut-http-client")
  implementation("io.micronaut:micronaut-runtime")
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("com.h2database:h2")
  runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
  testImplementation("io.micronaut:micronaut-http-client")
  testImplementation("io.mockk:mockk:1.13.8")
  testImplementation("io.projectreactor:reactor-test:3.6.0")
  testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

application { mainClass.set("io.github.architectplatform.data.ApplicationKt") }

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
    annotations("io.github.architectplatform.data.*")
  }
  aot {
    optimizeServiceLoading.set(false)
    convertYamlToJava.set(false)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
    deduceEnvironment.set(true)
    optimizeNetty.set(true)
  }
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("1.9.25")
    }
  }
}
