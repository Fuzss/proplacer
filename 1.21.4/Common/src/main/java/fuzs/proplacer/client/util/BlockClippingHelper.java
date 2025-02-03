package fuzs.proplacer.client.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockClippingHelper {

    public static boolean isBlockPositionInLine(Entity entity, double pickRange, BlockPos targetPos) {
        Vec3 startVector = entity.getEyePosition(0.0F);
        Vec3 viewVector = entity.getViewVector(0.0F);
        Vec3 endVector = startVector.add(viewVector.x() * pickRange, viewVector.y() * pickRange, viewVector.z() * pickRange);
        ClipContext clipContext = new ClipContext(startVector,
                endVector,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                entity
        );

        return isBlockPositionInLine(entity.level(), clipContext, targetPos);
    }

    /**
     * Checks if a given block position is in line for the given context (i.e. it is somewhere hit by the provided
     * vector). Takes block and fluid shapes in account, meaning does not pass through shapes in-between, as
     * {@link BlockGetter#isBlockInLine(ClipBlockStateContext)} would do.
     */
    public static boolean isBlockPositionInLine(BlockGetter blockGetter, ClipContext context, BlockPos targetPos) {
        // unboxing should never produce a NullPointerException here, as the method is guaranteed to return a non-null value from both functions (either success or fail)
        return BlockGetter.traverseBlocks(context.getFrom(),
                context.getTo(),
                context,
                (ClipContext traverseContext, BlockPos traversePos) -> {
                    if (traversePos.equals(targetPos)) {
                        return Boolean.TRUE;
                    } else {
                        BlockState blockState = blockGetter.getBlockState(traversePos);
                        FluidState fluidState = blockGetter.getFluidState(traversePos);
                        Vec3 from = traverseContext.getFrom();
                        Vec3 to = traverseContext.getTo();
                        VoxelShape blockShape = traverseContext.getBlockShape(blockState, blockGetter, traversePos);
                        BlockHitResult blockHitResult = blockGetter.clipWithInteractionOverride(from,
                                to,
                                traversePos,
                                blockShape,
                                blockState
                        );
                        if (blockHitResult != null) {
                            return Boolean.FALSE;
                        } else {
                            VoxelShape fluidShape = traverseContext.getFluidShape(fluidState, blockGetter, traversePos);
                            BlockHitResult fluidHitResult = fluidShape.clip(from, to, traversePos);
                            return fluidHitResult != null ? Boolean.FALSE : null;
                        }
                    }
                },
                (ClipContext failContext) -> {
                    return Boolean.FALSE;
                }
        );
    }
}
