package fuzs.multiloader.bytecode

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.Project

@Serializable
data class MappingsJson(val mappings: List<MappingEntry>)

@Serializable
data class MappingsJson2(val mappings: Map<String, Map<String, MappingEntry>>)

@Serializable
data class MappingEntry(val from: FieldMapping, val to: FieldMapping)

@Serializable
data class FieldMapping(val owner: String, val name: String, val descriptor: String? = null)

fun Project.loadConventionalTagsMappings(): Map<String, Map<String, FieldMapping>> {
    return object {}.javaClass.classLoader.getResourceAsStream("loader-mapping-neoforge3.json")
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
        ?.let {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<Map<String, Map<String, FieldMapping>>>(it)
        } ?: throw IllegalStateException("Unable to read loader-mapping-neoforge3.json from plugin resources")
}
