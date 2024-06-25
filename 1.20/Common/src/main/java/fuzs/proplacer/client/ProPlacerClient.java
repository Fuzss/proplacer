package fuzs.proplacer.client;

import fuzs.proplacer.client.handler.FastPlacementHandler;
import fuzs.proplacer.client.handler.KeyMappingHandler;
import fuzs.proplacer.client.handler.ReachAroundPlacementHandler;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.core.v1.context.KeyMappingsContext;
import fuzs.puzzleslib.api.client.event.v1.ClientTickEvents;
import fuzs.puzzleslib.api.client.event.v1.InteractionInputEvents;
import fuzs.puzzleslib.api.client.screen.v2.KeyMappingActivationHelper;
import fuzs.puzzleslib.api.event.v1.LoadCompleteCallback;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerInteractEvents;

public class ProPlacerClient implements ClientModConstructor {

    @Override
    public void onConstructMod() {
        registerEventHandlers();
    }

    private static void registerEventHandlers() {
        PlayerInteractEvents.USE_BLOCK.register(FastPlacementHandler.INSTANCE::onUseBlock);
        InteractionInputEvents.USE.register(ReachAroundPlacementHandler::onUseInteraction);
        ClientTickEvents.START.register(FastPlacementHandler.INSTANCE::onStartClientTick);
        ClientTickEvents.START.register(KeyMappingHandler::onStartClientTick);
        LoadCompleteCallback.EVENT.register(KeyMappingHandler::onLoadComplete);
    }

    @Override
    public void onRegisterKeyMappings(KeyMappingsContext context) {
        context.registerKeyMapping(KeyMappingHandler.KEY_TOGGLE_FAST_PLACEMENT, KeyMappingActivationHelper.KeyActivationContext.GAME);
    }
}
