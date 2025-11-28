package fuzs.multiloader.fabric

import fuzs.multiloader.extension.MultiLoaderExtension
import fuzs.multiloader.metadata.DependencyType
import fuzs.multiloader.metadata.EnvironmentProvider
import fuzs.multiloader.metadata.LinkProvider
import fuzs.multiloader.metadata.ModLoaderProvider
import metadata
import mod
import net.fabricmc.loom.api.fmj.FabricModJsonV1Spec
import net.fabricmc.loom.task.FabricModJsonV1Task
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import versionCatalog
import kotlin.jvm.optionals.getOrNull

fun Project.configureModJson(task: FabricModJsonV1Task) {
    val multiLoaderExtension = extensions.getByType(MultiLoaderExtension::class.java)
    task.outputFile.set(layout.buildDirectory.file("generated/resources/fabric.mod.json"))

    task.json {
        modId.set(mod.id)
        version.set(mod.version)
        name.set(mod.name)
        mod.authors.forEach { author(it) }
        description.set(mod.description)
        addDistributions(this)
        licenses.add(mod.license)
        icon("mod_logo.png")
        configureEnvironment(this)
        addEntrypoints(this)
        mixin("${mod.id}.common.mixins.json")
        mixin("${mod.id}.${this@configureModJson.name.lowercase()}.mixins.json")
        addDependencies(this)
        if (multiLoaderExtension.modFile.orNull?.library?.orNull == true) {
            customData.put("modmenu", mapOf("badges" to listOf("library")))
        }

        multiLoaderExtension.modFile.orNull?.json?.orNull?.execute(this)
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
    val packagePrefix = multiLoaderExtension.modFile.orNull?.packagePrefix?.orNull
        ?.takeIf { it.isNotEmpty() }
        ?.let { "$it." }
        ?: ""

    // Construct fully qualified class names for main and client entrypoints
    addIfExists("main", "${project.group}.${project.name.lowercase()}.${packagePrefix}${archivesName}Fabric")
    addIfExists(
        "client",
        "${project.group}.${project.name.lowercase()}.${packagePrefix}client.${archivesName}FabricClient"
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
