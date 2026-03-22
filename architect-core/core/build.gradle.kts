plugins {
  kotlin("jvm") version "1.9.25"
}

group = "io.github.architectplatform"
version = "1.6.1"

val kotlinVersion = project.properties.get("kotlinVersion") as String
val micronautVersion = project.properties.get("micronautVersion") as String
val jacksonVersion = project.properties.get("jacksonVersion") as String
val snakeyamlVersion = project.properties.get("snakeyamlVersion") as String

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
  implementation("io.github.architectplatform:api:2.1.0")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${jacksonVersion}")
  implementation("org.yaml:snakeyaml:${snakeyamlVersion}")
  implementation("org.slf4j:slf4j-api:2.0.13")

  compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
  compileOnly("io.micronaut:micronaut-runtime:${micronautVersion}")
  compileOnly("io.micronaut:micronaut-http:${micronautVersion}")
  compileOnly("io.micronaut:micronaut-http-client:${micronautVersion}")
  compileOnly("io.micronaut:micronaut-context:${micronautVersion}")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
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

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("1.9.25")
    }
  }
}
