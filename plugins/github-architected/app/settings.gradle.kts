rootProject.name = "github-architected"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("../../../architect-api/api") {
    dependencySubstitution {
        substitute(module("io.github.architectplatform:api")).using(project(":"))
    }
}
