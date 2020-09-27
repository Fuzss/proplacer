package com.fuzs.proplacer.client;

import com.fuzs.proplacer.util.AirRayTracer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class FastPlacementHandler {

    private final Minecraft mc = Minecraft.getInstance();

    private int rightClickDelayTimer;
    private RayTraceResult objectMouseOver;
    private Vector3d playerPos = Vector3d.ZERO;

    private BlockPos lastPos = BlockPos.ZERO;
    private Direction lastDir;
    private Hand lastHand;

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock evt) {

        // only run for blocks and on client
        if (evt.getFace() != null && evt.getPlayer().getHeldItem(evt.getHand()).getItem() instanceof BlockItem && evt.getWorld().isRemote) {

            // don't update in case previous block couldn't be placed
            if (this.lastPos != BlockPos.ZERO && evt.getWorld().isAirBlock(this.lastPos)) { // || new BlockItemUseContext(evt.getPlayer(), evt.getHand(), evt.getPlayer().getHeldItem(evt.getHand()), new BlockRayTraceResult(Vector3d.ZERO, evt.getFace(), evt.getPos(), false)).canPlace())) {

                return;
            }

            // where this block would be placed
            BlockPos placed = evt.getPos().offset(evt.getFace());
            this.lastDir = getDirectionToBlock(this.lastPos, placed).orElse(null);
            this.lastPos = placed;

            if (this.lastDir != null) {

                this.lastHand = evt.getHand();
            }
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onClickInput(InputEvent.ClickInputEvent evt) {

        // disable normal block placing while we're doing our own thing
        if (evt.isUseItem() && evt.getHand() == this.lastHand) {

            evt.setCanceled(true);
            evt.setSwingHand(false);
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END) {

            return;
        }

        if (!this.mc.gameSettings.keyBindUseItem.isKeyDown()) {

            this.reset();
            return;
        }

        if (this.rightClickDelayTimer > 0) {

            --this.rightClickDelayTimer;
        }

        if (this.lastHand == null) {

            this.handleInitialCooldown();
            return;
        }

        if (this.rightClickDelayTimer != 0) {

            return;
        }

        assert this.mc.player != null;
        if (!this.mc.player.getPositionVec().isWithinDistanceOf(this.playerPos, 0.5) && this.hasRayTraceTarget()) {

            this.playerPos = this.mc.player.getPositionVec();
            this.rightClickMouse();

            Entity entity = this.mc.getRenderViewEntity();
            assert entity != null && this.mc.playerController != null;
            Vector3d eyePosition = entity.getEyePosition(0.0F);
            // increase placing speed when getting too far away
            this.rightClickDelayTimer = eyePosition.isWithinDistanceOf(Vector3d.copyCentered(this.lastPos), this.mc.playerController.getBlockReachDistance() - 1.0F) ? 2 : 1;
        }
    }

    private void reset() {

        this.lastHand = null;
        this.lastDir = null;
        this.lastPos = BlockPos.ZERO;
        // setting to 9 as it'll be decreased by one in the same tick
        this.rightClickDelayTimer = 9;
        this.objectMouseOver = null;
        this.playerPos = Vector3d.ZERO;
    }

    private void handleInitialCooldown() {

        assert this.mc.player != null;
        if (this.playerPos == Vector3d.ZERO) {

            this.playerPos = this.mc.player.getPositionVec();
        }

        if (!this.mc.player.getPositionVec().isWithinDistanceOf(this.playerPos, 0.5)) {

            this.rightClickDelayTimer -= 4;
        } else if (this.rightClickDelayTimer < 7 && this.objectMouseOver instanceof BlockRayTraceResult && this.mc.objectMouseOver instanceof BlockRayTraceResult) {

            if (!((BlockRayTraceResult) this.objectMouseOver).getPos().equals(((BlockRayTraceResult) this.mc.objectMouseOver).getPos())) {

                this.rightClickDelayTimer -= 4;
            } else if (((BlockRayTraceResult) this.objectMouseOver).getFace() != ((BlockRayTraceResult) this.mc.objectMouseOver).getFace()) {

                this.rightClickDelayTimer -= 4;
            }
        }

        this.rightClickDelayTimer = Math.max(0, this.rightClickDelayTimer);
        this.mc.rightClickDelayTimer = this.rightClickDelayTimer;
        this.objectMouseOver = this.mc.objectMouseOver;
    }

    private boolean hasRayTraceTarget() {

        Entity entity = this.mc.getRenderViewEntity();
        assert this.mc.playerController != null && entity != null;

        double rayTraceDistance = this.mc.playerController.getBlockReachDistance();
        Vector3d startVec = entity.getEyePosition(0.0F);
        Vector3d look = entity.getLook(0.0F);
        Vector3d endVec = startVec.add(look.x * rayTraceDistance, look.y * rayTraceDistance, look.z * rayTraceDistance);
        RayTraceContext context = new RayTraceContext(startVec, endVec, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, entity);
        return AirRayTracer.rayTraceBlocks(entity.world, context, this.lastPos.offset(this.lastDir));
    }

    private void rightClickMouse() {

        assert this.mc.playerController != null && this.mc.player != null && this.mc.world != null && this.mc.objectMouseOver != null;

        this.mc.rightClickDelayTimer = 4;
        BlockPos pos = this.lastPos;
        Direction dir = this.lastDir;

        // take a block face the cursor might be pointing at into account, but only from blocks adjacent to ours
        if (this.mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK) {

            BlockPos posToBe = pos.offset(dir);
            BlockPos posRayTrace = ((BlockRayTraceResult) this.mc.objectMouseOver).getPos();
            dir = getDirectionToBlock(posRayTrace, posToBe).orElse(dir);
            pos = posToBe.offset(dir.getOpposite());
        }

        BlockRayTraceResult blockraytraceresult = new BlockRayTraceResult(this.mc.objectMouseOver.getHitVec(), dir, pos, false);
        ItemStack itemstack = this.mc.player.getHeldItem(this.lastHand);
        int count = itemstack.getCount();
        ActionResultType actionresulttype = this.mc.playerController.func_217292_a(this.mc.player, this.mc.world, this.lastHand, blockraytraceresult);
        if (actionresulttype.isSuccessOrConsume()) {

            if (actionresulttype.isSuccess()) {

                this.mc.player.swingArm(this.lastHand);
                if (!itemstack.isEmpty() && (itemstack.getCount() != count || this.mc.playerController.isInCreativeMode())) {

                    this.mc.gameRenderer.itemRenderer.resetEquippedProgress(this.lastHand);
                }
            }
        }
    }

    private static Optional<Direction> getDirectionToBlock(BlockPos faceFrom, BlockPos faceTo) {

        if (faceFrom.manhattanDistance(faceTo) == 1) {

            Vector3i dirVec = faceTo.subtract(faceFrom);
            return Optional.of(Direction.getFacingFromVector(dirVec.getX(), dirVec.getY(), dirVec.getZ()));
        }

        return Optional.empty();
    }

}
