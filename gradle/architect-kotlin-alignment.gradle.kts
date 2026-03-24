import org.gradle.api.artifacts.VersionCatalogsExtension

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val kotlinVersion = versionCatalog.findVersion("kotlin").get().requiredVersion
val coroutinesVersion = versionCatalog.findVersion("coroutines").get().requiredVersion

configurations.configureEach {
  if (name == "detekt") {
    return@configureEach
  }

  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
      useVersion(kotlinVersion)
      because("All Kotlin modules should use the shared repository Kotlin version")
    }

    if (requested.group == "org.jetbrains.kotlinx") {
      useVersion(coroutinesVersion)
      because("All Kotlin modules should use the shared repository coroutines version")
    }
  }
}