package com.fuzs.proplacer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FrontPlacementHandler {

    private final Minecraft mc = Minecraft.getInstance();

    //@SubscribeEvent
    public void onClickInput(InputEvent.ClickInputEvent evt) {

        if (evt.isUseItem()) {

            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.getType() == RayTraceResult.Type.MISS) {


            }

            evt.setCanceled(true);
            evt.setSwingHand(false);
        }
    }

}
