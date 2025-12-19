plugins { kotlin("jvm") version "2.3.0" }

group = "io.github.architectplatform.plugins"

version = "1.0.0"

java { sourceCompatibility = JavaVersion.toVersion("17") }

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

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
  implementation("io.github.architectplatform:api:1.1.3")

  testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.1")
}

tasks.test {
  useJUnitPlatform()
}
