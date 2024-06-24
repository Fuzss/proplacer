package fuzs.proplacer.client;

import fuzs.proplacer.client.util.BlockClippingHelper;
import fuzs.proplacer.mixin.client.accessor.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FastPlacementHandler {
    private final Minecraft minecraft = Minecraft.getInstance();
    @Nullable
    private BlockPos lastPos;
    @Nullable
    private Direction lastDirection;
    @Nullable
    private InteractionHand lastHand;
    @Nullable
    private Vec3 hitLocation;

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock evt) {

        if (evt.getLevel().isClientSide && evt.getFace() != null) {

            if (this.minecraft.player.getItemInHand(evt.getHand()).getItem() instanceof BlockItem) {

                if (this.lastPos == null || !evt.getLevel().isEmptyBlock(this.lastPos)) {

                    BlockPos pos = evt.getPos().relative(evt.getFace());
                    if (this.lastPos != null) {
                        this.lastDirection = getDirectionToBlock(this.lastPos, pos).orElse(null);
                    } else {
                        this.lastDirection = null;
                    }

                    this.lastPos = pos;
                    if (this.lastDirection != null) {
                        this.lastHand = evt.getHand();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onClickInput(InputEvent.InteractionKeyMappingTriggered evt) {
//        if (evt.isUseItem() && evt.getHand() == this.lastHand) {
//            evt.setCanceled(true);
//            evt.setSwingHand(false);
//        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent evt) {
        // needs to run at the beginning of client tick to avoid double placing in a single tick when vanilla has just placed a block
        if (evt.phase == TickEvent.Phase.START) {
            if (!this.minecraft.options.keyUse.isDown()) {
                this.reset();
            } else {

                if (this.lastHand == null) {
                    // store hit location once when locking placement direction, so that it is easier to place e.g. stairs consistently
                    this.hitLocation = this.minecraft.hitResult.getLocation();
                } else {
                    // always set this to default delay for blocking vanilla from running Minecraft::startUseItem
                    ((MinecraftAccessor) this.minecraft).proplacer$setRightClickDelay(4);
                    float pickRange = this.minecraft.gameMode.getPickRange();
                    BlockPos pos = this.lastPos.relative(this.lastDirection);
                    if (BlockClippingHelper.isBlockPositionInLine(this.minecraft.cameraEntity, pickRange, pos)) {
//                        this.rightClickMouse();

                        HitResult oldHitResult = this.minecraft.hitResult;
                        this.minecraft.hitResult = this.getBlockHitResult();
                        ((MinecraftAccessor) this.minecraft).proplacer$callStartUseItem();
//                        this.minecraft.hitResult = oldHitResult;
                    }
                }
            }
        }
    }

    private void reset() {
        this.lastHand = null;
        this.lastDirection = null;
        this.lastPos = null;
        this.hitLocation = null;
    }

    private void rightClickMouse() {

        ((MinecraftAccessor) this.minecraft).proplacer$setRightClickDelay(4);
        BlockHitResult hitResult = getBlockHitResult();
        ItemStack itemInHand = this.minecraft.player.getItemInHand(this.lastHand);
        int itemCount = itemInHand.getCount();
        InteractionResult interactionResult = this.minecraft.gameMode.useItemOn(this.minecraft.player,
                this.lastHand,
                hitResult
        );
        if (interactionResult.shouldSwing()) {
            this.minecraft.player.swing(this.lastHand);
            if (!itemInHand.isEmpty() &&
                    (itemInHand.getCount() != itemCount || this.minecraft.gameMode.hasInfiniteItems())) {
                this.minecraft.gameRenderer.itemInHandRenderer.itemUsed(this.lastHand);
            }
        }
    }

    private BlockHitResult getBlockHitResult() {

        BlockPos pos = this.lastPos;
        Direction direction = this.lastDirection;

        if (this.minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos offsetPos = pos.relative(direction);
            BlockPos posRayTrace = ((BlockHitResult) this.minecraft.hitResult).getBlockPos();
            direction = getDirectionToBlock(posRayTrace, offsetPos).orElse(direction);
            pos = offsetPos.relative(direction.getOpposite());
        }

        Vec3 hitLocation = new Vec3(pos.getX() + Mth.frac(this.hitLocation.x()),
                pos.getY() + Mth.frac(this.hitLocation.y()),
                pos.getZ() + Mth.frac(this.hitLocation.z())
        );

        return new BlockHitResult(hitLocation, direction, pos, false);
    }

    private static Optional<Direction> getDirectionToBlock(BlockPos from, BlockPos to) {
        if (from.distManhattan(to) == 1) {
            Vec3i vec3i = to.subtract(from);
            return Optional.of(Direction.getNearest(vec3i.getX(), vec3i.getY(), vec3i.getZ()));
        } else {
            return Optional.empty();
        }
    }
}
