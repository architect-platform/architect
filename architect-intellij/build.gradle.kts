plugins {
  id("org.jetbrains.intellij") version "1.17.0"
  kotlin("jvm") version "1.9.25"
}

group = "io.github.architectplatform"
version = "0.1.0"

repositories {
  mavenCentral()
}

intellij {
  version.set("2024.1")
  type.set("IC")
  plugins.set(listOf("com.intellij.java", "org.jetbrains.plugins.yaml"))
}

kotlin {
  jvmToolchain(17)
}

tasks {
  patchPluginXml {
    sinceBuild.set("241")
    untilBuild.set("251.*")
  }
}
