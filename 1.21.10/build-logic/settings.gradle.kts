dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // TODO move the file to the default location, then remove this as it's no longer necessary
            from(files("../gradle/libs.versions2.toml"))
        }
    }
}

val modName: String = providers.gradleProperty("mod.name").get()
rootProject.name = modName.replace(Regex("[^a-zA-Z]"), "")
