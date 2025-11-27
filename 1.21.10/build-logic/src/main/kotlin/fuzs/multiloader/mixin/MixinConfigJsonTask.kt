package fuzs.multiloader.mixin

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

abstract class MixinConfigJsonTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Nested
    abstract val config: MixinConfigJsonSpec

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun generateJson() {
        workerExecutor.noIsolation().submit(GenerateJsonWorkAction::class.java) {
            configProvider.set(config)
            outputFileProvider.set(outputFile)
        }
    }

    fun config(action: Action<MixinConfigJsonSpec>) {
        action.execute(config)
    }
}

abstract class GenerateJsonWorkAction : WorkAction<GenerateJsonParameters> {
    override fun execute() {
        val json = MixinConfigJsonGenerator.generate(parameters.configProvider.get())
        parameters.outputFileProvider.get().asFile.writeText(json)
    }
}

interface GenerateJsonParameters : WorkParameters {
    val configProvider: Property<MixinConfigJsonSpec>
    val outputFileProvider: RegularFileProperty
}
