package fuzs.proplacer.client;

import fuzs.proplacer.mixin.client.accessor.MinecraftAccessor;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.event.v1.ClientTickEvents;
import fuzs.puzzleslib.api.client.event.v1.InteractionInputEvents;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import fuzs.puzzleslib.api.event.v1.core.EventResultHolder;
import fuzs.puzzleslib.api.event.v1.entity.player.PlayerInteractEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public class ProPlacerClient implements ClientModConstructor {
    @Nullable
    private static HitResult lastHitResult;

    @Override
    public void onConstructMod() {
//        registerHandlers();
    }

    private static void registerEventHandlers() {
        InteractionInputEvents.USE.register((Minecraft minecraft, LocalPlayer player, InteractionHand interactionHand, HitResult hitResult) -> {
            if (lastHitResult != null && hitResult.getType() == HitResult.Type.MISS) {
                minecraft.hitResult = lastHitResult;
            }
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                ((MinecraftAccessor) minecraft).proplacer$setRightClickDelay(0);
            }
            return EventResult.PASS;
        });
        ClientTickEvents.END.register(minecraft -> {
            if (!minecraft.options.keyUse.isDown()) {
                lastHitResult = null;
            }
        });
        PlayerInteractEvents.USE_ITEM_V2.register((player, level, interactionHand) -> {
            if (level.isClientSide) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.hitResult.getType() != HitResult.Type.MISS) {
                    lastHitResult = minecraft.hitResult.getType() == HitResult.Type.ENTITY ? null : minecraft.hitResult;
                }
            }
            return EventResultHolder.pass();
        });
    }
}
