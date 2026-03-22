rootProject.name = "architect-core"

includeBuild("../../architect-api/api") {
  dependencySubstitution {
    substitute(module("io.github.architectplatform:api")).using(project(":"))
  }
}
