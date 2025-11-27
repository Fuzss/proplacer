package fuzs.multiloader.fabric

import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.EnvironmentProvider
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import fuzs.multiloader.extension.MultiLoaderExtension
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import metadata
import mod
import net.fabricmc.loom.api.fmj.FabricModJsonV1Spec
import net.fabricmc.loom.task.FabricModJsonV1Task
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import versionCatalog
import java.io.File
import kotlin.jvm.optionals.getOrNull

fun Project.configureModJson(task: FabricModJsonV1Task) {
    task.outputFile.set(layout.buildDirectory.file("generated/resources/fabric.mod.json"))
    val multiLoaderExtension = extensions.getByType(MultiLoaderExtension::class.java)

    task.json {
        modId.set(mod.id)
        version.set(mod.version)
        name.set(mod.name)
        mod.authors.forEach { author(it) }
        addDistributions(this)
        licenses.add(mod.license)
//        icon(135, "mod_logo.png")
        icon("mod_logo.png")
        configureEnvironment(this)
        addEntrypoints(this)
        addMixinConfigs(this)
        addDependencies(this)
        if (multiLoaderExtension.modFileMetadata.orNull?.library?.orNull == true) {
            customData.put("modmenu", mapOf("badges" to listOf("library")))
        }

        multiLoaderExtension.modFileMetadata.orNull?.json?.orNull?.execute(this)
    }
}

private fun Project.addDistributions(json: FabricModJsonV1Spec) {
    val githubUrl = metadata.links
        .firstOrNull { it.name == LinkProvider.GITHUB }
        ?.url()
    githubUrl?.let {
        json.contactInformation.putAll(
            mapOf(
                "homepage" to it,
                "sources" to it,
                "issues" to "${it}/issues"
            )
        )
    }
}

private fun Project.configureEnvironment(json: FabricModJsonV1Spec) {
    fun supportsEnvironment(env: EnvironmentProvider) =
        metadata.environments.any { it.name == env && it.type != DependencyType.UNSUPPORTED }

    json.environment.set(
        when {
            supportsEnvironment(EnvironmentProvider.CLIENT) && supportsEnvironment(EnvironmentProvider.SERVER) -> "*"
            supportsEnvironment(EnvironmentProvider.CLIENT) -> "client"
            supportsEnvironment(EnvironmentProvider.SERVER) -> "server"
            else -> error("No environments defined")
        }
    )
}

private fun Project.addEntrypoints(json: FabricModJsonV1Spec) {
    // Helper to only add an entrypoint if the class file exists
    fun addIfExists(type: String, className: String) {
        if (file("src/main/java/${className.replace('.', '/')}.java").exists()) {
            json.entrypoint(type, className)
        }
    }

    // Access the Base plugin extension for archivesName
    val baseExtension = extensions.getByType(BasePluginExtension::class.java)
    val archivesName = baseExtension.archivesName.get()
    val multiLoaderExtension = extensions.getByType(MultiLoaderExtension::class.java)
    val packagePrefix = multiLoaderExtension.modFileMetadata.orNull?.packagePrefix?.orNull
        ?.takeIf { it.isNotEmpty() }
        ?.let { "$it." }
        ?: ""

    // Construct fully qualified class names for main and client entrypoints
    addIfExists("main", "${group}.${name.lowercase()}.${packagePrefix}${archivesName}Fabric")
    addIfExists("client", "${group}.${name.lowercase()}.${packagePrefix}client.${archivesName}FabricClient")
}

private fun Project.addMixinConfigs(json: FabricModJsonV1Spec) {
    fun parseMixinConfig(file: File): Boolean {
        if (!file.exists()) return false
        val root = Json.Default.parseToJsonElement(file.readText()).jsonObject
        return listOf("mixins", "client", "server").any { key ->
            root[key]?.jsonArray?.isNotEmpty() == true
        }
    }

    fun addIfNonEmpty(name: String, file: File) {
        if (parseMixinConfig(file)) json.mixin(name)
    }

    addIfNonEmpty("${mod.id}.common.mixins.json", project(":Common").file("src/main/resources/common.mixins.json"))
    addIfNonEmpty(
        "${mod.id}.${name.lowercase()}.mixins.json",
        file("src/main/resources/${name.lowercase()}.mixins.json")
    )
}

private fun Project.addDependencies(json: FabricModJsonV1Spec) {
    fun incrementPatch(version: String): String {
        val parts = version.split(".").toMutableList()
        when {
            parts.size < 2 -> throw IllegalArgumentException("Version must have at least MAJOR.MINOR")
            parts.size == 2 -> parts.add("0")
        }

        parts[parts.lastIndex] = (parts.last().toInt() + 1).toString()
        return parts.joinToString(".")
    }

    fun versionOrAny(alias: String) =
        versionCatalog.findVersion(alias).getOrNull()?.requiredVersion?.let { ">=${it}" } ?: "*"

    json.depends(
        "minecraft",
        versionCatalog.findVersion("minecraft").get().requiredVersion.let {
            ">=${it}- <${incrementPatch(it)}-"
        }
    )
    json.depends("fabricloader", versionOrAny("fabricloader.min"))

    for (entry in metadata.dependencies) {
        if (entry.type != DependencyType.UNSUPPORTED && (entry.platform == ModLoaderProvider.COMMON || entry.platform == ModLoaderProvider.FABRIC)) {
            when (entry.name) {
                "fabricapi" -> json.depends("fabric-api", versionOrAny("fabricapi.min"))
                "puzzleslib" -> json.depends("puzzleslib", versionOrAny("puzzleslib.min"))
                else -> json.depends(entry.name, "*")
            }
        }
    }
}
