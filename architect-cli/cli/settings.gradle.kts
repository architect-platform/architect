rootProject.name = "architect-cli"

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../../gradle/libs.versions.toml"))
		}
	}
}

includeBuild("../../architect-core/core") {
	dependencySubstitution {
		substitute(module("io.github.architectplatform:architect-core")).using(project(":"))
	}
}

includeBuild("../../architect-api/api") {
	dependencySubstitution {
		substitute(module("io.github.architectplatform:api")).using(project(":"))
	}
}
