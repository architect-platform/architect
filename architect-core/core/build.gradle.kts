plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
  id("info.solidsoft.pitest") version "1.15.0"
  id("me.champeau.jmh") version "0.7.2"
}

group = "io.github.architectplatform"
version = "1.6.1"

val kotlinVersion = project.properties.get("kotlinVersion") as String
val jacksonVersion = project.properties.get("jacksonVersion") as String
val snakeyamlVersion = project.properties.get("snakeyamlVersion") as String
val coroutinesVersion = project.properties.get("coroutinesVersion") as String

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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${jacksonVersion}")
  implementation("org.yaml:snakeyaml:${snakeyamlVersion}")
  implementation("org.slf4j:slf4j-api:2.0.13")
  implementation("com.networknt:json-schema-validator:1.5.6")

  compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

  jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
  jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
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

jacoco { toolVersion = "0.8.12" }

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
