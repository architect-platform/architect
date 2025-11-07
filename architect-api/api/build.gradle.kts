plugins {
  kotlin("jvm") version "1.9.25"
  `maven-publish`
  id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
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
  // Test dependencies
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.14.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.14.1")
}

tasks.test {
  useJUnitPlatform()
}

ktlint {
  version.set("1.0.1")
  verbose.set(true)
  android.set(false)
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
