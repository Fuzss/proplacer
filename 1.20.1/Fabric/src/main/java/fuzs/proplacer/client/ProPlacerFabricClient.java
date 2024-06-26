package fuzs.proplacer.client;

import fuzs.proplacer.ProPlacer;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import net.fabricmc.api.ClientModInitializer;

public class ProPlacerFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientModConstructor.construct(ProPlacer.MOD_ID, ProPlacerClient::new);
    }
}
