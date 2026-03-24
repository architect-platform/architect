rootProject.name = "terraform-architected"

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("../../../gradle/libs.versions.toml"))
		}
	}
}
