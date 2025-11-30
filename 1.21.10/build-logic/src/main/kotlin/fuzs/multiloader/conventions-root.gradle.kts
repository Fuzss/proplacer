package fuzs.multiloader

import fuzs.multiloader.metadata.loadMetadata
import fuzs.multiloader.task.IncrementBuildNumber
import kotlinx.serialization.json.Json

afterEvaluate {
    val metadata = loadMetadata()
    val json = Json { prettyPrint = true }
    val output = project.layout.projectDirectory.file("metadata.json").asFile
    output.writeText(json.encodeToString(metadata))
}

tasks.register<IncrementBuildNumber>("incrementBuildNumber") {
    val propertiesFile = layout.buildDirectory.file("build.properties")
    // Check file existence here, as the property requires it when set.
    if (propertiesFile.orNull?.asFile?.exists() == true) {
        inputFile.set(propertiesFile)
    }

    outputFile.set(propertiesFile)
}
