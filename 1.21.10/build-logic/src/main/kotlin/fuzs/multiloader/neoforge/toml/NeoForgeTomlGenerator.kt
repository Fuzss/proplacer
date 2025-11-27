package fuzs.multiloader.neoforge.toml

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule

object NeoForgeTomlGenerator {
    fun generate(spec: NeoForgeModsTomlSpec): String {
        val input = NeoForgeModsToml(
            spec.modLoader.orNull,
            spec.loaderVersion.orNull,
            spec.license.get(),
            spec.showAsResourcePack.orNull,
            spec.showAsDataPack.orNull,
            spec.services.orNull?.takeIf { it.isNotEmpty() },
            spec.properties.orNull?.takeIf { it.isNotEmpty() },
            spec.issueTrackerURL.orNull,
            spec.mods.orNull?.map {
                ModEntry(
                    it.modId.get(),
                    it.namespace.orNull,
                    it.version.orNull,
                    it.displayName.orNull,
                    it.description.orNull,
                    it.logoFile.orNull,
                    it.logoBlur.orNull,
                    it.updateJSONURL.orNull,
                    it.modUrl.orNull,
                    it.credits.orNull,
                    it.authors.orNull,
                    it.displayURL.orNull,
                    it.enumExtensions.orNull,
                    it.featureFlags.orNull
                )
            },
            spec.features.orNull?.takeIf { it.isNotEmpty() }
                ?.map { it.modId.get() to it.properties.get() }
                ?.fold(mutableMapOf<String, MutableMap<String, Any>>()) { accumulator, (key, value) ->
                    accumulator.merge(key, value.toMutableMap()) { old, new ->
                        old.apply { putAll(new) }
                    }
                    accumulator
                },
            spec.modProperties.orNull?.takeIf { it.isNotEmpty() }
                ?.map { it.modId.get() to it.properties.get() }
                ?.fold(mutableMapOf<String, MutableMap<String, Any>>()) { accumulator, (key, value) ->
                    accumulator.merge(key, value.toMutableMap()) { old, new ->
                        old.apply { putAll(new) }
                    }
                    accumulator
                },
            spec.accessTransformers.orNull?.takeIf { it.isNotEmpty() }?.map { AccessTransformerEntry(it.file.get()) },
            spec.mixins.orNull?.takeIf { it.isNotEmpty() }
                ?.map {
                    MixinEntry(
                        it.config.get(),
                        it.requiredMods.orNull?.takeIf { it.isNotEmpty() },
                        it.behaviorVersion.orNull
                    )
                },
            spec.dependencies.orNull?.takeIf { it.isNotEmpty() }?.map {
                it.modId.get() to DependencyEntry(
                    it.properties.modId.get(),
                    it.properties.type.orNull,
                    it.properties.reason.orNull,
                    it.properties.versionRange.orNull,
                    it.properties.ordering.orNull,
                    it.properties.side.orNull,
                    it.properties.referralUrl.orNull
                )
            }
                ?.groupBy({ it.first }, { it.second }),
            spec.extraProperties.orNull?.takeIf { it.isNotEmpty() }
                ?.map { it.property.get() to it.properties.get() }
                ?.fold(mutableMapOf<String, MutableMap<String, Any>>()) { accumulator, (key, value) ->
                    accumulator.merge(key, value.toMutableMap()) { old, new ->
                        old.apply { putAll(new) } // merge inner maps
                    }
                    accumulator
                },
            spec.extraArrayProperties.orNull?.takeIf { it.isNotEmpty() }
                ?.map { it.property.get() to it.properties.get() }
                ?.groupBy({ it.first }, { it.second })
        )

        val toml = Toml(serializersModule = SerializersModule {
            contextual(Any::class, AnyPrimitiveSerializer)
        })

        return buildString {
            appendLine(
                toml.encodeToString(
                    NeoForgeModsToml.serializer(),
                    input
                )
            )
            input.extraProperties?.let {
                appendLine()
                appendLine(toml.encodeToString(it))
            }
            input.extraArrayProperties?.let {
                appendLine()
                appendLine(toml.encodeToString(it))
            }
        }
    }
}
