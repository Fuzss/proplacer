package fuzs.multiloader.plugin

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.VersionCatalogBuilder
import java.net.URI
import kotlin.collections.iterator

class SettingsConventionPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) = with(settings) {

        val modName: String = providers.gradleProperty("mod.name").get()
        val projectLibs: String = providers.gradleProperty("project.libs").get()
        val pluginLibs: String = providers.gradleProperty("project.libs.plugins").get()
        val platforms: String? = providers.gradleProperty("project.platforms").orNull

        rootProject.name = modName.replace(Regex("[^A-Za-z]"), "") + "-" + projectLibs.replace(Regex("-v\\d+"), "")
        val platformsList = platforms?.split(",")?.map { it.trim() }?.distinct() ?: emptyList()
        platformsList.forEach { include(it) }
        settings.plugins.apply("org.gradle.toolchains.foojay-resolver-convention")

        dependencyResolutionManagement {
            repositories {
                maven {
                    name = "Fuzs Mod Resources"
                    url = URI("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
                }
            }

            versionCatalogs {
                create("libs") {
                    from("fuzs.sharedcatalogs:sharedcatalogs-libraries:$projectLibs")
                    overrideKeys("project.libs", settings)
                }

                create("pluginLibs") {
                    from("fuzs.sharedcatalogs:sharedcatalogs-plugins:$pluginLibs")
                    overrideKeys("project.libs", settings)
                }
            }
        }
    }
}

private fun VersionCatalogBuilder.overrideKeys(
    prefix: String,
    settings: Settings
) {
    for ((key, value) in settings.extensions.extraProperties.properties) {
        when {
            key.startsWith("$prefix.versions.") -> {
                val name = key.removePrefix("$prefix.versions.").replace(".", "-")
                version(name, value.toString())
            }

            key.startsWith("$prefix.plugins.") -> {
                val name = key.removePrefix("$prefix.plugins.").replace(".", "-")
                val parts = value.toString().split(":", limit = 2)
                if (parts.size == 2) {
                    plugin(name, parts[0]).version(parts[1])
                }
            }

            key.startsWith("$prefix.libraries.") -> {
                val name = key.removePrefix("$prefix.libraries.").replace(".", "-")
                library(name, value.toString())
            }
        }
    }
}
