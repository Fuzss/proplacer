package fuzs.proplacer.neoforge;

import fuzs.proplacer.ProPlacer;
import fuzs.puzzleslib.api.core.v1.ModConstructor;
import net.neoforged.fml.common.Mod;

@Mod(ProPlacer.MOD_ID)
public class ProPlacerNeoForge {

    public ProPlacerNeoForge() {
        System.out.println(ProPlacer.IS_AQUATIC);
        ModConstructor.construct(ProPlacer.MOD_ID, ProPlacer::new);
    }
}
