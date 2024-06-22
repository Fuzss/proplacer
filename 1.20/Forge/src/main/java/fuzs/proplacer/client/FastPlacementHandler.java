package fuzs.proplacer.client;

import fuzs.proplacer.mixin.client.accessor.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
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
    private final Minecraft mc = Minecraft.getInstance();
    private int rightClickDelay;
    @Nullable
    private HitResult hitResult;
    private Vec3 playerPosition = Vec3.ZERO;
    private BlockPos lastPos = BlockPos.ZERO;
    @Nullable
    private Direction lastDirection;
    @Nullable
    private InteractionHand lastHand;

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock evt) {

        if (evt.getLevel().isClientSide && evt.getFace() != null &&
                this.mc.player.getItemInHand(evt.getHand()).getItem() instanceof BlockItem) {

            if (this.lastPos == BlockPos.ZERO || !evt.getLevel().isEmptyBlock(this.lastPos)) {

                BlockPos pos = evt.getPos().relative(evt.getFace());
                this.lastDirection = getDirectionToBlock(this.lastPos, pos).orElse(null);
                this.lastPos = pos;

                if (this.lastDirection != null) {
                    this.lastHand = evt.getHand();
                }
            }
        }
    }

    @SubscribeEvent
    public void onClickInput(InputEvent.InteractionKeyMappingTriggered evt) {
        if (evt.isUseItem() && evt.getHand() == this.lastHand) {
            evt.setCanceled(true);
            evt.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END) {
            if (!this.mc.options.keyUse.isDown()) {
                this.reset();
            } else {
                if (this.rightClickDelay > 0) {
                    --this.rightClickDelay;
                }

                if (this.lastHand == null) {
                    this.handleInitialCooldown();
                } else if (this.rightClickDelay == 0) {
                    if (this.hasRayTraceTarget()) {
                        this.rightClickMouse();
                    }
                }
            }
        }
    }

    private void reset() {
        this.lastHand = null;
        this.lastDirection = null;
        this.lastPos = BlockPos.ZERO;
        // one less than vanilla default as this runs at the end of tick, while vanilla updates the value at the beginning, so it would already be at 9
        this.rightClickDelay = 9;
        this.hitResult = null;
        this.playerPosition = Vec3.ZERO;
    }

    private void handleInitialCooldown() {

        if (this.playerPosition == Vec3.ZERO) {

            this.playerPosition = this.mc.player.position();
        }

        if (!this.mc.player.position().closerThan(this.playerPosition, 0.5)) {
            // reduce timer when further away so block placing speed can keep up with player movement
            this.rightClickDelay -= 4;
        } else if (this.rightClickDelay < 7 && this.hitResult.getType() == HitResult.Type.BLOCK &&
                this.mc.hitResult.getType() == HitResult.Type.BLOCK) {

            BlockHitResult currentHitResult = (BlockHitResult) this.mc.hitResult;
            BlockHitResult lastGoodHitResult = (BlockHitResult) this.hitResult;

            if (!lastGoodHitResult.getBlockPos().equals(currentHitResult.getBlockPos())) {
                this.rightClickDelay -= 4;
            } else if (lastGoodHitResult.getDirection() != currentHitResult.getDirection()) {
                this.rightClickDelay -= 4;
            }
        }

        this.rightClickDelay = Math.max(0, this.rightClickDelay);
        ((MinecraftAccessor) this.mc).proplacer$setRightClickDelay(this.rightClickDelay);
        this.hitResult = this.mc.hitResult;
    }

    private boolean hasRayTraceTarget() {
        Entity entity = this.mc.getCameraEntity();
        double pickRange = this.mc.gameMode.getPickRange();
        Vec3 startVec = entity.getEyePosition(0.0F);
        Vec3 viewVector = entity.getViewVector(0.0F);
        Vec3 endVec = startVec.add(viewVector.x() * pickRange, viewVector.y() * pickRange, viewVector.z() * pickRange);
        ClipContext clipContext = new ClipContext(startVec,
                endVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                entity
        );
        return AirRayTracer.rayTraceBlocks(entity.level(), clipContext, this.lastPos.relative(this.lastDirection));
    }

    private void rightClickMouse() {

        ((MinecraftAccessor) this.mc).proplacer$setRightClickDelay(4);
        BlockPos pos = this.lastPos;
        Direction direction = this.lastDirection;
        if (this.mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos offsetPos = pos.relative(direction);
            BlockPos posRayTrace = ((BlockHitResult) this.mc.hitResult).getBlockPos();
            direction = getDirectionToBlock(posRayTrace, offsetPos).orElse(direction);
            pos = offsetPos.relative(direction.getOpposite());
        }

        Vec3 hitLocation = this.hitResult.getLocation();
        Vec3 newHitLocation = new Vec3(pos.getX() + Mth.frac(hitLocation.x()),
                pos.getY() + Mth.frac(hitLocation.y()),
                pos.getZ() + Mth.frac(hitLocation.z())
        );
        BlockHitResult hitResult = new BlockHitResult(newHitLocation, direction, pos, false);
        ItemStack itemInHand = this.mc.player.getItemInHand(this.lastHand);
        int itemCount = itemInHand.getCount();
        InteractionResult interactionResult = this.mc.gameMode.useItemOn(this.mc.player, this.lastHand, hitResult);
        if (interactionResult.shouldSwing()) {
            this.mc.player.swing(this.lastHand);
            if (!itemInHand.isEmpty() && (itemInHand.getCount() != itemCount || this.mc.gameMode.hasInfiniteItems())) {
                this.mc.gameRenderer.itemInHandRenderer.itemUsed(this.lastHand);
            }
        }
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
