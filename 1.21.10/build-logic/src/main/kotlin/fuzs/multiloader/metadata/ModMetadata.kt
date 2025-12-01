package fuzs.multiloader.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project

@Serializable
data class ModMetadata(
    val mod: ModEntry,
    val dependencies: List<DependencyEntry>,
    val links: List<DistributionEntry>,
    val environments: EnvironmentsEntry,
    val platforms: List<ModLoaderProvider>
)

@Serializable
data class ModEntry(
    val id: String,
    val group: String,
    val name: String,
    val version: String,
    val authors: List<String>,
    val description: String,
    val license: String
)

@Serializable
data class DependencyEntry(
    val name: String,
    val platform: ModLoaderProvider,
    val type: DependencyType
)

@Serializable
data class DistributionEntry(
    val name: LinkProvider,
    val slug: String,
    val id: String? = null
) {
    fun url(): String = name.url(slug)
}

@Serializable
data class EnvironmentsEntry(
    val client: DependencyType,
    val server: DependencyType
)

enum class ModLoaderProvider(val platform: Boolean = true) {
    @SerialName("common")
    COMMON(false) {
        override fun matches(modLoader: ModLoaderProvider): Boolean = true
    },

    @SerialName("fabric")
    FABRIC,

    @SerialName("neoforge")
    NEOFORGE;

    open fun matches(modLoader: ModLoaderProvider): Boolean = this == modLoader
}

enum class DependencyType(val required: Boolean) {
    @SerialName("required")
    REQUIRED(true),

    @SerialName("embedded")
    EMBEDDED(true),

    @SerialName("optional")
    OPTIONAL(false),

    @SerialName("unsupported")
    UNSUPPORTED(false)
}

enum class LinkProvider(val baseUrl: String) {
    @SerialName("github")
    GITHUB("https://github.com/Fuzss/"),

    @SerialName("curseforge")
    CURSEFORGE("https://www.curseforge.com/minecraft/mc-mods/"),

    @SerialName("modrinth")
    MODRINTH("https://modrinth.com/mod/");

    fun url(slug: String): String = "${baseUrl}${slug}"
}

fun Project.loadMetadata(): ModMetadata {

    val properties: Map<String, String> =
        (rootProject.properties + project.properties).mapValues { it.value.toString() }

    fun collect(prefix: String): Map<String, String> =
        properties
            .filterKeys { it.startsWith(prefix) }
            .mapKeys { it.key.removePrefix(prefix) }

    val dependencyProperties = collect("dependencies.")
    val distributionProperties = collect("distributions.")
    val environmentProperties = collect("environments.")

    val dependencies = dependencyProperties.map { (key, value) ->
        DependencyEntry(
            key.substringAfter('.'),
            ModLoaderProvider.valueOf(key.substringBefore('.').uppercase()),
            DependencyType.valueOf(value.uppercase())
        )
    }

    val distributions = distributionProperties
        .entries
        .groupBy { it.key.substringBefore('.') }
        .map { (name, entries) ->
            val map = entries.associate { it.key.substringAfter('.') to it.value }
            DistributionEntry(
                LinkProvider.valueOf(name.uppercase()),
                map["slug"]!!,
                map["id"]
            )
        }

    val environments =
        EnvironmentsEntry(
            environmentProperties["client"]?.uppercase()?.let { DependencyType.valueOf(it) }
                ?: DependencyType.UNSUPPORTED,
            environmentProperties["server"]?.uppercase()?.let { DependencyType.valueOf(it) }
                ?: DependencyType.UNSUPPORTED
        ).also {
            require(it.client != DependencyType.UNSUPPORTED || it.server != DependencyType.UNSUPPORTED) { "No environments defined" }
        }

    val platformList = properties["project.platforms"]
        ?.split(',')
        ?.map { it.trim() } ?: emptyList()

    val platforms = platformList.mapNotNull {
        runCatching { ModLoaderProvider.valueOf(it.uppercase()) }.getOrNull()?.takeIf { it.platform }
    }

    val authorList = properties["mod.authors"]
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: error("No authors defined")

    return ModMetadata(
        ModEntry(
            properties["mod.id"]!!,
            properties["mod.group"]!!,
            properties["mod.name"]!!,
            properties["mod.version"]!!,
            authorList,
            properties["mod.description"]!!,
            properties["mod.license"]!!
        ),
        dependencies,
        distributions,
        environments,
        platforms
    )
}
