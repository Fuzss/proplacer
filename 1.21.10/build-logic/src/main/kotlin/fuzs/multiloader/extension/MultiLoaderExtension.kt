package fuzs.multiloader.extension

import fuzs.multiloader.mixin.MixinConfigJsonSpec
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class MultiLoaderExtension {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Input
    @get:Optional
    abstract val modFileMetadata: Property<ModFileMetadataExtension>

    @get:Input
    @get:Optional
    abstract val mixinConfig: Property<MixinConfigJsonSpec>

    fun modFileMetadata(configure: Action<ModFileMetadataExtension>) {
        objects.newInstance(ModFileMetadataExtension::class.java).also { configure.execute(it) }
    }

    fun mixinConfig(configure: Action<MixinConfigJsonSpec>) {
        objects.newInstance(MixinConfigJsonSpec::class.java).also { configure.execute(it) }
    }
}
