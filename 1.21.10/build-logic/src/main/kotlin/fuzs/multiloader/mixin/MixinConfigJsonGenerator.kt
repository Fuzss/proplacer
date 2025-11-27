package fuzs.multiloader.mixin

import kotlinx.serialization.json.Json

object MixinConfigJsonGenerator {
    fun generate(spec: MixinConfigJsonSpec): String {
        val input = MixinConfigJson(
            parent = spec.parent.orNull,
            target = spec.target.orNull,
            minVersion = spec.minVersion.orNull,
            requiredFeatures = spec.requiredFeatures.orNull?.takeIf { it.isNotEmpty() },
            compatibilityLevel = spec.compatibilityLevel.orNull,
            required = spec.required.orNull,
            priority = spec.priority.orNull,
            mixinPriority = spec.mixinPriority.orNull,
            mixinPackage = spec.mixinPackage.get(),
            mixins = spec.mixins.orNull?.takeIf { it.isNotEmpty() },
            client = spec.client.orNull?.takeIf { it.isNotEmpty() },
            server = spec.server.orNull?.takeIf { it.isNotEmpty() },
            setSourceFile = spec.setSourceFile.orNull,
            refmap = spec.refmap.orNull,
            refmapWrapper = spec.refmapWrapper.orNull,
            verbose = spec.verbose.orNull,
            plugin = spec.plugin.orNull,
            injectors = spec.injectors.orNull?.let {
                InjectorsEntry(
                    defaultRequire = it.defaultRequire.orNull,
                    defaultGroup = it.defaultGroup.orNull,
                    namespace = it.namespace.orNull,
                    injectionPoints = it.injectionPoints.orNull?.takeIf { it.isNotEmpty() },
                    dynamicSelectors = it.dynamicSelectors.orNull?.takeIf { it.isNotEmpty() },
                    maxShiftBy = it.maxShiftBy.orNull
                )
            },
            overwrites = spec.overwrites.orNull?.let {
                OverwritesEntry(
                    conformVisibility = it.conformVisibility.orNull,
                    requireAnnotations = it.requireAnnotations.orNull
                )
            }
        )

        val json = Json { prettyPrint = true }
        return json.encodeToString(input)
    }
}
