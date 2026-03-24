plugins {
  kotlin("jvm") version "1.9.25"
  jacoco
}

version = libs.versions.pluginDefaultArtifact.get()

apply(from = "../../../gradle/architect-plugin-conventions.gradle.kts")
