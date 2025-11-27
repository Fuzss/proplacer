import fuzs.multiloader.metadata.ModEntry
import fuzs.multiloader.metadata.ModMetadata
import fuzs.multiloader.metadata.loadMetadata
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// Expose the libs version catalog
val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Load metadata once per project
val Project.metadata: ModMetadata
    get() = extensions.findByType(ModMetadata::class.java) ?: loadMetadata().also {
        extensions.add(ModMetadata::class.java, "metadata", it)
    }

// Expose the mod entry from metadata
val Project.mod: ModEntry
    get() = metadata.mod
