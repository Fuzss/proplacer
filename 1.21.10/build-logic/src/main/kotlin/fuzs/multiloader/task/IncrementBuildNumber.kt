package fuzs.multiloader.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.*

abstract class IncrementBuildNumber : DefaultTask() {
    @get:InputFile
    @get:Optional
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val file = inputFile.asFile.get()
        val properties = Properties().apply { if (file.exists()) load(file.inputStream()) }
        val uniqueBuildNumber = properties.getProperty("uniqueBuildNumber")?.toIntOrNull() ?: 0
        properties.setProperty("uniqueBuildNumber", (uniqueBuildNumber + 1).toString())
        outputFile.asFile.get().outputStream().use { properties.store(it, null) }
    }
}
