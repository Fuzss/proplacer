import fuzs.multiloader.metadata.*
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType

// Expose the libs version catalog
val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Load external mods once per project
val Project.externalMods: ExternalMods
    get() = extensions.findByType(ExternalMods::class.java) ?: run {
        val modsMap = object {}.javaClass.classLoader.getResourceAsStream("dependencies.json")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?.let {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<Map<String, ExternalModMetadata>>(it)
            } ?: throw IllegalStateException("Unable to read dependencies.json from plugin resources")
        ExternalMods(modsMap)
    }.also {
        extensions.add(ExternalMods::class.java, "externalMods", it)
    }

// Load metadata once per project
val Project.metadata: ModMetadata
    get() = extensions.findByType(ModMetadata::class.java) ?: loadMetadata().also {
        extensions.add(ModMetadata::class.java, "metadata", it)
    }

// Expose the mod entry from metadata
val Project.mod: ModEntry
    get() = metadata.mod

// Load the fabric.loom.dontRemap Loom property that disables remapping outputs (useful for the common subproject)
val Project.dontRemap: Boolean
    get() = extra.has("fabric.loom.dontRemap") && (extra["fabric.loom.dontRemap"] as? String)?.toBoolean() == true

// Load the loom.platform Architectury Loom property
val Project.projectPlatform: ModLoaderProvider
    get() = (if (extra.has("loom.platform")) extra["loom.platform"] as? String else null)?.uppercase()
        ?.let { ModLoaderProvider.valueOf(it) } ?: ModLoaderProvider.COMMON

fun Project.expectPlatform(platform: ModLoaderProvider) {
    if (platform != project.projectPlatform) {
        throw GradleException("Mismatched platform: ${project.projectPlatform} != ${platform}, define via loom.platform=${platform} in gradle.properties")
    }
}

// Get the Common project from the Loom property
val Project.commonProject: Project
    get() = rootProject.subprojects.firstOrNull { !it.projectPlatform.platform }!!

// Get the platform projects from the Loom property
val Project.platformProjects: List<Project>
    get() = rootProject.subprojects.filter { it.projectPlatform.platform }
