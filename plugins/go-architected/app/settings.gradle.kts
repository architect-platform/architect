rootProject.name = "go-architected"

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../../../gradle/libs.versions.toml"))
		}
	}
}
