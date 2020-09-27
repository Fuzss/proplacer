package com.fuzs.proplacer.util;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;

import java.util.function.Function;

public class AirRayTracer {

    public static boolean rayTraceBlocks(IBlockReader reader, RayTraceContext context, BlockPos search) {

        Vector3d startVec = context.getStartVec();
        Vector3d endVec = context.getEndVec();
        return !startVec.equals(endVec) && doRayTrace(startVec, endVec, blockPos ->
                rayTracer(reader, context, blockPos), search);
    }

    private static BlockRayTraceResult rayTracer(IBlockReader reader, RayTraceContext context, BlockPos blockPos) {

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

    private static boolean doRayTrace(Vector3d startVec, Vector3d endVec, Function<BlockPos, BlockRayTraceResult> rayTracer, BlockPos search) {

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
