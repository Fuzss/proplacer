package fuzs.proplacer.client.handler;

import fuzs.proplacer.client.util.BlockClippingHelper;
import fuzs.proplacer.mixin.client.accessor.MinecraftAccessor;
import fuzs.puzzleslib.api.event.v1.core.EventResultHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FastPlacementHandler extends AbstractFastBlockHandler {
    public static final FastPlacementHandler INSTANCE = new FastPlacementHandler();

    @Nullable
    private Vec3 hitLocation;
    @Nullable
    private InteractionHand interactionHand;

    public EventResultHolder<InteractionResult> onUseBlock(Player player, Level level, InteractionHand interactionHand, BlockHitResult hitResult) {

        if (level.isClientSide) {

            // using block place context allows for supporting e.g. placing slabs inside other slabs
            // this logic seems to work ideal for our use-case, with Block::canBeReplaced called internally
            BlockPlaceContext context = new BlockPlaceContext(player,
                    interactionHand,
                    player.getItemInHand(interactionHand),
                    hitResult
            );
            this.setNewBlockPos(context.getClickedPos());
            this.interactionHand = interactionHand;
        }

        return EventResultHolder.pass();
    }

    @Override
    protected void tickNonActive(Minecraft minecraft) {
        if (minecraft.hitResult != null) {
            // store hit location once when locking placement direction, so that it is easier to place e.g. stairs consistently
            this.hitLocation = minecraft.hitResult.getLocation();
        }
    }

    @Override
    protected void tickWhenActive(Minecraft minecraft) {
        // always set this to default delay for blocking vanilla from running Minecraft::startUseItem
        ((MinecraftAccessor) minecraft).proplacer$setRightClickDelay(4);
        if (BlockClippingHelper.isBlockPositionInLine(minecraft.cameraEntity, minecraft.player.blockInteractionRange(), this.getTargetPosition())) {

            Vec3 hitLocation = new Vec3(this.blockPos.getX() + Mth.frac(this.hitLocation.x()),
                    this.blockPos.getY() + Mth.frac(this.hitLocation.y()),
                    this.blockPos.getZ() + Mth.frac(this.hitLocation.z())
            );
            BlockHitResult hitResult = new BlockHitResult(hitLocation, this.direction, this.blockPos, false);
            ReachAroundPlacementHandler.startUseItemWithSecondaryUseActive(minecraft,
                    minecraft.player,
                    this.interactionHand,
                    hitResult
            );
        }
    }

    @Override
    public boolean isActive() {
        return super.isActive() && this.hitLocation != null;
    }

    @Override
    public void clear() {
        super.clear();
        this.hitLocation = null;
        this.interactionHand = null;
    }

    @Override
    protected KeyMapping getKeyMapping(Options options) {
        return options.keyUse;
    }

    @Override
    protected boolean requireEmptyBlock() {
        return false;
    }
}
