plugins {
  alias(libs.plugins.kotlin.jvm)
  `maven-publish`
  alias(libs.plugins.ktlint)
  alias(libs.plugins.detekt)
  jacoco
  alias(libs.plugins.pitest)
}

group = "io.github.architectplatform"

version = "2.1.0"

java {
  withSourcesJar()
  withJavadocJar()
  sourceCompatibility = JavaVersion.toVersion("17")
}

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

repositories { mavenCentral() }

dependencies {
  implementation(libs.kotlin.reflect)
  // Exposed for ArchitectPluginContractTestSuite (consumed by plugin tests)
  api(libs.junit.jupiter.api)

  // Test dependencies
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
  useJUnitPlatform()
}

ktlint {
  version.set("1.0.1")
  verbose.set(true)
  android.set(false)
}

detekt {
  config.setFrom(rootProject.file("../../detekt.yml"))
  buildUponDefaultConfig = true
  ignoreFailures = true
  baseline = file("detekt-baseline.xml")
}

publishing {
  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
      artifactId = "api"
      pom {
        name.set("Architect API")
        description.set("API for the Architect engine")
        url.set("https://github.com/architect-platform/architect")
        licenses {
          license {
            name.set("Apache-2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            id.set("alemazzo")
            name.set("Alessandro Mazzoli")
          }
        }
      }
    }
  }
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/architect-platform/architect")
      credentials {
        username =
          System.getenv("GITHUB_USER")
            ?: project.findProperty("githubUser") as String?
            ?: "github-actions"
        password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("githubToken") as String?
      }
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

pitest {
  junit5PluginVersion.set("1.2.1")
  targetClasses.set(listOf("io.github.architectplatform.api.*"))
  targetTests.set(listOf("io.github.architectplatform.api.*"))
  mutators.set(listOf("DEFAULTS"))
  outputFormats.set(listOf("HTML", "XML"))
  timestampedReports.set(false)
  threads.set(Runtime.getRuntime().availableProcessors())
}
