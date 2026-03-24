version = libs.versions.architectCliArtifact.get()

group = "io.github.architectplatform"

apply(from = "../../gradle/architect-kotlin-alignment.gradle.kts")

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.allopen)
  alias(libs.plugins.shadow.legacy)
  alias(libs.plugins.micronaut.application)
  alias(libs.plugins.micronaut.aot)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.detekt)
  jacoco
}

repositories { mavenCentral() }

dependencies {
  implementation(libs.architect.api.contract)
  implementation(libs.architect.core.module)
  kapt("info.picocli:picocli-codegen")
  kapt(libs.micronaut.serde.processor)
  implementation("info.picocli:picocli")
  implementation(libs.micronaut.kotlin.runtime)
  implementation("io.micronaut.picocli:micronaut-picocli")
  implementation(libs.micronaut.serde.jackson)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.micronaut.reactor)
  implementation(libs.micronaut.http.client)
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("org.yaml:snakeyaml")
}

application { mainClass = "io.github.architectplatform.cli.ArchitectLauncher" }

graalvmNative.toolchainDetection.set(false)

graalvmNative {
  binaries {
    named("main") {
      buildArgs.add("-J-Xmx4g")
    }
  }
}

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
  ignoreFailures = true
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
