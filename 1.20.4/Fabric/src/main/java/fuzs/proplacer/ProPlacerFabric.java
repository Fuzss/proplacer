package fuzs.proplacer;

import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.fabricmc.api.ModInitializer;

public class ProPlacerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConstructor.construct(ProPlacer.MOD_ID, ProPlacer::new);
    }
}
