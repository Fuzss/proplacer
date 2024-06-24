package fuzs.proplacer.mixin.client.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("rightClickDelay")
    void proplacer$setRightClickDelay(int rightClickDelay);

    @Invoker("startUseItem")
    void proplacer$callStartUseItem();
}
