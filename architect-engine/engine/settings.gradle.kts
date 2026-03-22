rootProject.name =
    "architect-engine"

includeBuild("../../architect-core/core") {
	dependencySubstitution {
		substitute(module("io.github.architectplatform:architect-core")).using(project(":"))
	}
}

includeBuild("../../architect-cli/cli") {
	dependencySubstitution {
		substitute(module("io.github.architectplatform:architect-cli")).using(project(":"))
	}
}

/*
                      includeBuild("../../architect-api/api") {
                      	dependencySubstitution {
                      		substitute(module("io.github.architectplatform:architect-api"))
                      			.using(project(":"))  // correct Kotlin DSL call
                      	}
                      }
                      */
