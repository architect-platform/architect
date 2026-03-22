rootProject.name = "architect-cli"

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
