package fuzs.multiloader.neoforge

import externalMods
import fuzs.multiloader.extension.MultiLoaderExtension
import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlSpec
import fuzs.multiloader.neoforge.toml.NeoForgeModsTomlTask
import metadata
import mod
import versionCatalog
import kotlin.jvm.optionals.getOrNull

fun NeoForgeModsTomlTask.setupModsTomlTask() {
    val multiLoaderExtension = project.extensions.getByType(MultiLoaderExtension::class.java)
    outputFile.set(project.layout.buildDirectory.file("generated/resources/META-INF/neoforge.mods.toml"))

    toml {
        val githubUrl = project.metadata.links
            .firstOrNull { it.name == LinkProvider.GITHUB }
            ?.url()

        license.set(project.mod.license)
        githubUrl?.let { issueTrackerURL.set("${it}/issues") }

        mod {
            modId.set(project.mod.id)
            displayName.set(project.mod.name)
            description.set(project.mod.description)
            version.set(project.mod.version)
            authors.set(project.mod.authors.joinToString(", "))
            logoFile.set("mod_logo.png")
            githubUrl?.let {
                modUrl.set(it)
                displayURL.set(it)
            }

            updateJSONURL.set("https://raw.githubusercontent.com/Fuzss/modresources/main/update/${project.mod.id}.json")
            multiLoaderExtension.modFile.orNull?.enumExtensions?.orNull?.let { enumExtensions.set(it) }
        }

        mixin("${project.mod.id}.common.mixins.json")
        mixin("${project.mod.id}.${project.name.lowercase()}.mixins.json")
        addDependencies()
        multiLoaderExtension.modFile.orNull?.toml?.orNull?.execute(this)
    }
}

private fun NeoForgeModsTomlTask.addDependencies() {
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
        project.versionCatalog.findVersion(alias).getOrNull()?.requiredVersion?.let { "[${it},)" }

    toml {
        dependency(
            project.mod.id
        ) {
            modId.set("minecraft")
            versionRange.set(project.versionCatalog.findVersion("minecraft").get().requiredVersion.let {
                "[${it},${incrementPatch(it)})"
            })
        }
        dependency(project.mod.id) {
            modId.set("neoforge")
            version("neoforge.min")?.let { versionRange.set(it) }
        }

        for (entry in project.metadata.dependencies) {
            if (entry.platform.matches(ModLoaderProvider.NEOFORGE)) {
                val modId = project.externalMods.mods[entry.name]?.mod?.id ?: entry.name
                when (entry.name) {
                    "puzzleslib" -> dependency(project.mod.id) {
                        this.modId.set(modId)
                        version("puzzleslib.min")?.let { versionRange.set(it) }
                    }

                    else -> dependency(project.mod.id) {
                        this.modId.set(modId)
                        when {
                            entry.type.required -> Unit
                            entry.type == DependencyType.OPTIONAL -> type.set(NeoForgeModsTomlSpec.DependencySpec.Type.OPTIONAL)
                            entry.type == DependencyType.UNSUPPORTED -> type.set(NeoForgeModsTomlSpec.DependencySpec.Type.INCOMPATIBLE)
                        }
                    }
                }
            }
        }
    }
}
