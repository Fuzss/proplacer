package fuzs.proplacer.neoforge.client;

import fuzs.proplacer.ProPlacer;
import fuzs.proplacer.client.ProPlacerClient;
import fuzs.proplacer.data.client.ModLanguageProvider;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.neoforge.api.data.v2.core.DataProviderHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

@Mod.EventBusSubscriber(modid = ProPlacer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ProPlacerNeoForgeClient {

    @SubscribeEvent
    public static void onConstructMod(final FMLConstructModEvent evt) {
        ClientModConstructor.construct(ProPlacer.MOD_ID, ProPlacerClient::new);
        DataProviderHelper.registerDataProviders(ProPlacer.MOD_ID, ModLanguageProvider::new);
    }
}
