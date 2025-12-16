package fuzs.proplacer.client.handler;

import fuzs.proplacer.mixin.client.accessor.MultiPlayerGameModeAccessor;
import fuzs.puzzleslib.api.event.v1.core.EventResult;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FastBreakingHandler extends AbstractFastBlockHandler {
    public static final FastBreakingHandler INSTANCE = new FastBreakingHandler();

    public EventResult onAttackBlock(Player player, Level level, InteractionHand interactionHand, BlockPos pos, Direction direction) {

        if (level.isClientSide() && player.getAbilities().instabuild) this.setNewBlockPos(pos);

        return EventResult.PASS;
    }

    @Override
    protected void tickNonActive(Minecraft minecraft) {
        // NO-OP
    }

    @Override
    protected void tickWhenActive(Minecraft minecraft) {
        // we run at the beginning of the client tick, so this is not updated yet
        // there does not seem to exist a better hook after this is updated, but before keybindings are processed
        minecraft.gameRenderer.pick(1.0F);
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK
                && ((BlockHitResult) minecraft.hitResult).getBlockPos().equals(this.getTargetPosition())) {

            // ignore Minecraft::missTime, it does not apply for creative mode
            ((MultiPlayerGameModeAccessor) minecraft.gameMode).proplacer$setDestroyDelay(0);
        }
    }

    @Override
    protected KeyMapping getKeyMapping(Options options) {
        return options.keyAttack;
    }

    @Override
    protected boolean requireEmptyBlock() {
        return true;
    }
}
