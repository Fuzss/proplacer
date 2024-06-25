package fuzs.proplacer.client.handler;

import fuzs.proplacer.client.util.BlockClippingHelper;
import fuzs.proplacer.mixin.client.accessor.MinecraftAccessor;
import fuzs.puzzleslib.api.event.v1.core.EventResultHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FastPlacementHandler {
    public static final FastPlacementHandler INSTANCE = new FastPlacementHandler();

    @Nullable
    private BlockPos blockPos;
    @Nullable
    private BlockPos newBlockPos;
    @Nullable
    private Direction direction;
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
            this.newBlockPos = context.getClickedPos();
            this.interactionHand = interactionHand;
        }

        return EventResultHolder.pass();
    }

    public void onStartClientTick(Minecraft minecraft) {

        // needs to run at the beginning of client tick to avoid double placing in a single tick when vanilla has just placed a block
        if (!KeyMappingHandler.isFastPlacementActive() ||
                minecraft.player != null && minecraft.player.onGround() && minecraft.player.isShiftKeyDown() ||
                !minecraft.options.keyUse.isDown()) {

            this.clear();
        } else {

            this.tickNewPosition(minecraft.level);
            if (!this.isFastPlacing()) {

                // store hit location once when locking placement direction, so that it is easier to place e.g. stairs consistently
                this.hitLocation = minecraft.hitResult.getLocation();
            } else {

                // always set this to default delay for blocking vanilla from running Minecraft::startUseItem
                ((MinecraftAccessor) minecraft).proplacer$setRightClickDelay(4);
                float pickRange = minecraft.gameMode.getPickRange();
                BlockPos targetPos = this.blockPos.relative(this.direction);
                if (BlockClippingHelper.isBlockPositionInLine(minecraft.cameraEntity, pickRange, targetPos)) {

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
        }
    }

    public boolean isFastPlacing() {
        return this.hitLocation != null && this.direction != null;
    }

    public void clear() {
        this.blockPos = null;
        this.direction = null;
        this.hitLocation = null;
        this.interactionHand = null;
    }

    private void tickNewPosition(Level level) {

        if (this.newBlockPos != null) {

            // prevent setting block position to the next one when the last placing attempt was unsuccessful,
            // e.g. when the player was standing in the way
            if (!level.isEmptyBlock(this.newBlockPos)) {

                if (this.blockPos != null && this.blockPos.distManhattan(this.newBlockPos) == 1) {

                    // find the direction we block are being placed in, must not match with the clicked block face,
                    // since the last placed block must not necessarily be what the next block is placed against
                    BlockPos diff = this.newBlockPos.subtract(this.blockPos);
                    this.direction = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
                } else {

                    this.direction = null;
                }

                this.blockPos = this.newBlockPos;
            }

            this.newBlockPos = null;
        }
    }
}
