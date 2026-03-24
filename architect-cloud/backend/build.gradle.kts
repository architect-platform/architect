plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.ksp)
  alias(libs.plugins.shadow)
  alias(libs.plugins.micronaut.application)
  alias(libs.plugins.micronaut.aot)
  jacoco
}

version = "1.0.0"

group = "io.github.architectplatform"

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
  ksp(libs.micronaut.http.validation)
  ksp(libs.micronaut.serde.processor)
  ksp(libs.micronaut.data.processor)
  implementation(libs.micronaut.kotlin.runtime)
  implementation(libs.micronaut.serde.jackson)
  implementation(libs.micronaut.data.jdbc)
  implementation(libs.micronaut.jdbc.hikari)
  implementation(libs.micronaut.reactor)
  implementation(libs.micronaut.websocket)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.reactor)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.micronaut.http.client)
  implementation(libs.micronaut.runtime)
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("com.h2database:h2")
  runtimeOnly(libs.jackson.module.kotlin)
  testImplementation(libs.micronaut.http.client)
  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.mockito.core)
  testImplementation(libs.archunit.junit5)
}

application { mainClass.set("io.github.architectplatform.cloud.ApplicationKt") }

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
    annotations("io.github.architectplatform.cloud.*")
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
      useVersion(libs.versions.kotlin.get())
    }
    if (requested.group == "org.jetbrains.kotlinx") {
      useVersion(libs.versions.coroutines.get())
    }
  }
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
