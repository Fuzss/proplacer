package fuzs.proplacer.fabric;

import fuzs.proplacer.ProPlacer;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;

public class ProPlacerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println(ConventionalBiomeTags.AQUATIC);
        ModConstructor.construct(ProPlacer.MOD_ID, ProPlacer::new);
    }
}
