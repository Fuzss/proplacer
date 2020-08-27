package com.fuzs.proplacer;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;
import java.util.function.Function;

public class FastPlacementHandler {

    private final Minecraft mc = Minecraft.getInstance();

    private int rightClickDelayTimer;
    private RayTraceResult objectMouseOver;
    private Vector3d playerLook = Vector3d.ZERO;

    private BlockPos lastPos = BlockPos.ZERO;
    private Direction lastDir;
    private Hand lastHand;

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock evt) {

        assert this.mc.player == null;

        // only run for blocks and on client
        if (evt.getFace() != null && this.mc.player.getHeldItem(evt.getHand()).getItem() instanceof BlockItem && evt.getWorld().isRemote) {

            // don't update in case previous block couldn't be placed
            if (this.lastPos != BlockPos.ZERO && evt.getWorld().isAirBlock(this.lastPos)) {

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

        if (this.hasRayTraceTarget()) {

            this.rightClickMouse();

//            Entity entity = this.mc.getRenderViewEntity();
//            assert entity != null && this.mc.playerController != null;
//            Vector3d eyePosition = entity.getEyePosition(0.0F);
//            // increase placing speed when getting too far away
//            this.rightClickDelayTimer = eyePosition.isWithinDistanceOf(Vector3d.copyCentered(this.lastPos), this.mc.playerController.getBlockReachDistance() - 1.0F) ? 2 : 1;
        }
    }

    private void reset() {

        this.lastHand = null;
        this.lastDir = null;
        this.lastPos = BlockPos.ZERO;
        // setting to 9 as it'll be decreased by one in the same tick
        this.rightClickDelayTimer = 9;
        this.objectMouseOver = null;
        this.playerLook = Vector3d.ZERO;
    }

    private void handleInitialCooldown() {

        assert this.mc.player != null;
        if (this.playerLook == Vector3d.ZERO) {

            this.playerLook = this.mc.player.getPositionVec();
        }

        if (!this.mc.player.getPositionVec().isWithinDistanceOf(this.playerLook, 0.5)) {

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
        return this.rayTraceBlocks(entity.world, context, this.lastPos.offset(this.lastDir));
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

    private boolean rayTraceBlocks(IBlockReader reader, RayTraceContext context, BlockPos search) {

        Vector3d startVec = context.getStartVec();
        Vector3d endVec = context.getEndVec();
        return !startVec.equals(endVec) && this.doRayTrace(startVec, endVec, blockPos ->
                this.rayTracer(reader, context, blockPos), search);
    }

    private BlockRayTraceResult rayTracer(IBlockReader reader, RayTraceContext context, BlockPos blockPos) {

        BlockState blockstate = reader.getBlockState(blockPos);
        FluidState fluidstate = reader.getFluidState(blockPos);
        Vector3d vector3d = context.getStartVec();
        Vector3d vector3d1 = context.getEndVec();
        VoxelShape voxelshape = context.getBlockShape(blockstate, reader, blockPos);
        BlockRayTraceResult blockraytraceresult = reader.rayTraceBlocks(vector3d, vector3d1, blockPos, voxelshape, blockstate);
        VoxelShape voxelshape1 = context.getFluidShape(fluidstate, reader, blockPos);
        BlockRayTraceResult blockraytraceresult1 = voxelshape1.rayTrace(vector3d, vector3d1, blockPos);
        double d0 = blockraytraceresult == null ? Double.MAX_VALUE : context.getStartVec().squareDistanceTo(blockraytraceresult.getHitVec());
        double d1 = blockraytraceresult1 == null ? Double.MAX_VALUE : context.getStartVec().squareDistanceTo(blockraytraceresult1.getHitVec());
        return d0 <= d1 ? blockraytraceresult : blockraytraceresult1;
    }

    private boolean doRayTrace(Vector3d startVec, Vector3d endVec, Function<BlockPos, BlockRayTraceResult> rayTracer, BlockPos search) {

        double d0 = MathHelper.lerp(-1.0E-7D, endVec.x, startVec.x);
        double d1 = MathHelper.lerp(-1.0E-7D, endVec.y, startVec.y);
        double d2 = MathHelper.lerp(-1.0E-7D, endVec.z, startVec.z);
        double d3 = MathHelper.lerp(-1.0E-7D, startVec.x, endVec.x);
        double d4 = MathHelper.lerp(-1.0E-7D, startVec.y, endVec.y);
        double d5 = MathHelper.lerp(-1.0E-7D, startVec.z, endVec.z);
        int i = MathHelper.floor(d3);
        int j = MathHelper.floor(d4);
        int k = MathHelper.floor(d5);

        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(i, j, k);
        BlockRayTraceResult blockraytraceresult = rayTracer.apply(blockpos$mutable);
        if (blockraytraceresult != null) {

            return false;
        }

        if (blockpos$mutable.equals(search)) {

            return true;
        }

        double d6 = d0 - d3;
        double d7 = d1 - d4;
        double d8 = d2 - d5;
        int l = MathHelper.signum(d6);
        int i1 = MathHelper.signum(d7);
        int j1 = MathHelper.signum(d8);
        double d9 = l == 0 ? Double.MAX_VALUE : (double)l / d6;
        double d10 = i1 == 0 ? Double.MAX_VALUE : (double)i1 / d7;
        double d11 = j1 == 0 ? Double.MAX_VALUE : (double)j1 / d8;
        double d12 = d9 * (l > 0 ? 1.0D - MathHelper.frac(d3) : MathHelper.frac(d3));
        double d13 = d10 * (i1 > 0 ? 1.0D - MathHelper.frac(d4) : MathHelper.frac(d4));
        double d14 = d11 * (j1 > 0 ? 1.0D - MathHelper.frac(d5) : MathHelper.frac(d5));

        while(d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {

            if (d12 < d13) {
                if (d12 < d14) {
                    i += l;
                    d12 += d9;
                } else {
                    k += j1;
                    d14 += d11;
                }
            } else if (d13 < d14) {
                j += i1;
                d13 += d10;
            } else {
                k += j1;
                d14 += d11;
            }

            blockraytraceresult = rayTracer.apply(blockpos$mutable.setPos(i, j, k));
            if (blockraytraceresult != null) {

                return false;
            }

            if (blockpos$mutable.equals(search)) {

                return true;
            }
        }

        return false;
    }

}
