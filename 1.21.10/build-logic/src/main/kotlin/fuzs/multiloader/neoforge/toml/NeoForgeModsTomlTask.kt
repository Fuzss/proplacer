package fuzs.multiloader.neoforge.toml

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class NeoForgeModsTomlTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Nested
    abstract val toml: NeoForgeModsTomlSpec

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun generateToml() {
        workerExecutor.noIsolation().submit(GenerateTomlWorkAction::class.java) {
            tomlProvider.set(toml)
            outputFileProvider.set(outputFile)
        }
    }

    fun toml(action: Action<NeoForgeModsTomlSpec>) {
        action.execute(toml)
    }
}

abstract class GenerateTomlWorkAction : WorkAction<GenerateTomlParameters> {
    override fun execute() {
        val toml = NeoForgeTomlGenerator.generate(parameters.tomlProvider.get())
        parameters.outputFileProvider.get().asFile.writeText(toml)
    }
}

interface GenerateTomlParameters : WorkParameters {
    val tomlProvider: Property<NeoForgeModsTomlSpec>
    val outputFileProvider: RegularFileProperty
}
