package fuzs.multiloader

import fuzs.multiloader.metadata.loadMetadata
import kotlinx.serialization.json.Json

afterEvaluate {
    val metadata = loadMetadata()
    val json = Json { prettyPrint = true }
    val output = project.layout.projectDirectory.file("metadata.json").asFile
    output.writeText(json.encodeToString(metadata))
}
