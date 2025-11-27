package fuzs.multiloader.neoforge

import fuzs.multiloader.extension.MultiLoaderExtension
import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlSpec
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import metadata
import mod
import org.gradle.api.Project
import versionCatalog
import java.io.File
import kotlin.jvm.optionals.getOrNull

fun Project.configureModsToml(task: NeoForgeModsTomlTask) {
    task.outputFile.set(layout.buildDirectory.file("generated/resources/META-INF/neoforge.mods.toml"))
    val multiLoaderExtension = extensions.getByType(MultiLoaderExtension::class.java)

    task.toml {
        val githubUrl = metadata.links
            .firstOrNull { it.name == LinkProvider.GITHUB }
            ?.url()

        license.set(mod.license)
        githubUrl?.let { issueTrackerURL.set("${it}/issues") }

        mod {
            modId.set(mod.id)
            displayName.set(mod.name)
            description.set(mod.description)
            version.set(mod.version)
            authors.set(mod.authors.joinToString(", "))
            logoFile.set("mod_logo.png")
            githubUrl?.let {
                modUrl.set(it)
                displayURL.set(it)
            }
            updateJSONURL.set("https://raw.githubusercontent.com/Fuzss/modresources/main/update/${mod.id}.json")
            multiLoaderExtension.modFileMetadata.orNull?.enumExtensions?.orNull?.let { enumExtensions.set(it) }
        }

        addMixinConfigs(this)
        addDependencies(this)
        multiLoaderExtension.modFileMetadata.orNull?.toml?.orNull?.execute(this)
    }
}

private fun Project.addMixinConfigs(toml: NeoForgeModsTomlSpec) {
    fun parseMixinConfig(file: File): Boolean {
        if (!file.exists()) return false
        val root = Json.Default.parseToJsonElement(file.readText()).jsonObject
        return listOf("mixins", "client", "server").any { key ->
            root[key]?.jsonArray?.isNotEmpty() == true
        }
    }

    fun addIfNonEmpty(name: String, file: File) {
        if (parseMixinConfig(file)) toml.mixin(name)
    }

    addIfNonEmpty("${mod.id}.common.mixins.json", project(":Common").file("src/main/resources/common.mixins.json"))
    addIfNonEmpty(
        "${mod.id}.${name.lowercase()}.mixins.json",
        file("src/main/resources/${name.lowercase()}.mixins.json")
    )
}

private fun Project.addDependencies(toml: NeoForgeModsTomlSpec) {
    fun incrementPatch(version: String): String {
        val parts = version.split(".").toMutableList()
        when {
            parts.size < 2 -> throw IllegalArgumentException("Version must have at least MAJOR.MINOR")
            parts.size == 2 -> parts.add("0")
        }

        parts[parts.lastIndex] = (parts.last().toInt() + 1).toString()
        return parts.joinToString(".")
    }

    fun version(alias: String): String? =
        versionCatalog.findVersion(alias).getOrNull()?.requiredVersion?.let { "[${it},)" }

    toml.dependency(
        mod.id
    ) {
        modId.set("minecraft")
        versionRange.set(versionCatalog.findVersion("minecraft").get().requiredVersion.let {
            "[${it},${incrementPatch(it)})"
        })
    }
    toml.dependency(mod.id) {
        modId.set("neoforge")
        version("neoforge.min")?.let { versionRange.set(it) }
    }

    for (entry in metadata.dependencies) {
        if (entry.type != DependencyType.UNSUPPORTED && (entry.platform == ModLoaderProvider.COMMON || entry.platform == ModLoaderProvider.NEOFORGE)) {
            when (entry.name) {
                "puzzleslib" -> toml.dependency(mod.id) {
                    modId.set("puzzleslib")
                    version("puzzleslib.min")?.let { versionRange.set(it) }
                }

                else -> toml.dependency(mod.id) {
                    modId.set(entry.name)
                }
            }
        }
    }
}
