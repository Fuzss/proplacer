package fuzs.proplacer.neoforge.client;

import fuzs.proplacer.ProPlacer;
import fuzs.proplacer.client.ProPlacerClient;
import fuzs.proplacer.data.client.ModLanguageProvider;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.neoforge.api.data.v2.core.DataProviderHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = ProPlacer.MOD_ID, dist = Dist.CLIENT)
public class ProPlacerNeoForgeClient {

    public ProPlacerNeoForgeClient() {
        ClientModConstructor.construct(ProPlacer.MOD_ID, ProPlacerClient::new);
        DataProviderHelper.registerDataProviders(ProPlacer.MOD_ID, ModLanguageProvider::new);
    }
}
