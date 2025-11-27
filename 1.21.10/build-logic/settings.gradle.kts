dependencyResolutionManagement {
    val pluginLibs: String = providers.gradleProperty("project.libs.plugins").get()

    repositories {
        maven {
            name = "Fuzs Mod Resources"
            url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
        }
    }

    versionCatalogs {
        create("pluginLibs") {
            from("fuzs.sharedcatalogs:sharedcatalogs-plugins:${pluginLibs}")
        }
    }
}
