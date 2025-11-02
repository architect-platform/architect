plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.25"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
  id("com.google.devtools.ksp") version "1.9.25-1.0.20"
  id("com.gradleup.shadow") version "8.3.5"
  id("io.micronaut.application") version "4.6.1"
  id("io.micronaut.aot") version "4.6.1"
}

version = "1.6.0"

group = "io.github.architectplatform"

val kotlinVersion = project.properties.get("kotlinVersion")

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
  implementation("io.github.architectplatform:api:1.2.0")
  ksp("io.micronaut:micronaut-http-validation")
  ksp("io.micronaut.serde:micronaut-serde-processor")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("io.micronaut.serde:micronaut-serde-jackson")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  compileOnly("io.micronaut:micronaut-http-client")
  implementation("io.micronaut:micronaut-runtime")
  runtimeOnly("io.micronaut:micronaut-http-client")
  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
  runtimeOnly("org.yaml:snakeyaml")
  testImplementation("io.micronaut:micronaut-http-client")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
  testImplementation("org.mockito:mockito-core:5.7.0")
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
