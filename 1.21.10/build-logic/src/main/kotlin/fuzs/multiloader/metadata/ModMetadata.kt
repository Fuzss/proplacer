package fuzs.multiloader.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project

@Serializable
data class ModMetadata(
    val mod: ModEntry,
    val dependencies: List<DependencyEntry>,
    val links: List<DistributionEntry>,
    val environments: List<EnvironmentEntry>,
    val platforms: List<ModLoaderProvider>
)

@Serializable
data class ModEntry(
    val id: String,
    val namespace: String? = null,
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
data class EnvironmentEntry(
    val name: EnvironmentProvider,
    val type: DependencyType
)

enum class ModLoaderProvider(val platform: Boolean = true) {
    @SerialName("common")
    COMMON(false),

    @SerialName("fabric")
    FABRIC,

    @SerialName("forge")
    FORGE,

    @SerialName("neoforge")
    NEOFORGE
}

enum class EnvironmentProvider {
    @SerialName("client")
    CLIENT,

    @SerialName("server")
    SERVER
}

enum class DependencyType {
    @SerialName("required")
    REQUIRED,

    @SerialName("optional")
    OPTIONAL,

    @SerialName("unsupported")
    UNSUPPORTED
}

enum class LinkProvider(val baseUrl: String) {
    @SerialName("github")
    GITHUB("https://github.com/Fuzss/"),

    @SerialName("curseforge")
    CURSEFORGE("https://www.curseforge.com/minecraft/mc-mods/"),

    @SerialName("modrinth")
    MODRINTH("https://modrinth.com/mod/");

    fun url(slug: String?): String = "${baseUrl}${slug ?: ""}"
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
            name = key.substringAfter('.'),
            platform = ModLoaderProvider.valueOf(key.substringBefore('.').uppercase()),
            type = DependencyType.valueOf(value.uppercase())
        )
    }

    val distributions = distributionProperties
        .entries
        .groupBy { it.key.substringBefore('.') }
        .map { (name, entries) ->
            val map = entries.associate { it.key.substringAfter('.') to it.value }
            DistributionEntry(
                name = LinkProvider.valueOf(name.uppercase()),
                slug = map["slug"] ?: "",
                id = map["id"]
            )
        }

    val environments = environmentProperties.map { (name, type) ->
        EnvironmentEntry(
            name = EnvironmentProvider.valueOf(name.uppercase()),
            type = DependencyType.valueOf(type.uppercase())
        )
    }.also {
        require(it.isNotEmpty()) { "No environments defined" }
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
            id = properties["mod.id"]!!,
            group = properties["mod.group"]!!,
            name = properties["mod.name"]!!,
            version = properties["mod.version"]!!,
            authors = authorList,
            description = properties["mod.description"]!!,
            license = properties["mod.license"]!!
        ),
        platforms = platforms,
        dependencies = dependencies,
        links = distributions,
        environments = environments
    )
}
