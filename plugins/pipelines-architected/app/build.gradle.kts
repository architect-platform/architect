plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

version = libs.versions.pluginDefaultArtifact.get()

apply(from = "../../../gradle/architect-plugin-conventions.gradle.kts")

repositories {
  mavenLocal()
}

dependencies {
  implementation(libs.slf4j.api)
  implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

}
