rootProject.name = "scripts-architected"

includeBuild("../../../architect-api/api") {
	dependencySubstitution {
		substitute(module("io.github.architectplatform:api")).using(project(":"))
	}
}
