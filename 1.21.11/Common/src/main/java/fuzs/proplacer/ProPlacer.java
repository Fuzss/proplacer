package fuzs.proplacer;

import fuzs.proplacer.config.ClientConfig;
import fuzs.puzzleslib.api.config.v3.ConfigHolder;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import fuzs.puzzleslib.api.core.v1.utility.ResourceLocationHelper;
import fuzs.puzzleslib.api.init.v3.tags.TagFactory;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class ProPlacer implements ModConstructor {
    public static final String MOD_ID = "proplacer";
    public static final String MOD_NAME = "Pro Placer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static final ConfigHolder CONFIG = ConfigHolder.builder(MOD_ID).client(ClientConfig.class);
    public static final TagKey<Biome> IS_AQUATIC = TagFactory.COMMON.registerBiomeTag("is_aquatic");

    public static ResourceLocation id(String path) {
        System.out.println(IS_AQUATIC);
        return ResourceLocationHelper.fromNamespaceAndPath(MOD_ID, path);
    }

    public <T> void prepareTag(ResourceKey<? extends Registry<? super T>> registryKey, TagKey<T> tagKey) {
        Objects.requireNonNull(registryKey, "registry key is null");
        Objects.requireNonNull(tagKey, "tag key is null");
        ResourceKey<? extends Registry<T>> registryKey1 = (ResourceKey<? extends Registry<T>>) registryKey;
        BuiltInRegistries.acquireBootstrapRegistrationLookup(getRegistry(registryKey).orElseThrow()).getOrThrow(tagKey);
    }

    public static <T> Optional<Registry<T>> getRegistry(ResourceKey<? extends Registry<? super T>> registryKey) {
        Objects.requireNonNull(registryKey, "registry key is null");
        return ((Registry<Registry<T>>) BuiltInRegistries.REGISTRY).getOptional((ResourceKey<Registry<T>>) registryKey);
    }
}
