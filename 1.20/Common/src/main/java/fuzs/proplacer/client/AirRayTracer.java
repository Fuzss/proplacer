package fuzs.proplacer.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

import java.util.function.Function;

public class AirRayTracer {

    public static boolean rayTraceBlocks(LevelReader reader, ClipContext context, BlockPos target) {
        Vec3 startVec = context.getFrom();
        Vec3 endVec = context.getTo();
        return !startVec.equals(endVec) && doRayTrace(startVec, endVec, (blockPos) -> {
            return rayTracer(reader, context, blockPos);
        }, target);
    }

    private static BlockHitResult rayTracer(LevelReader reader, ClipContext context, BlockPos blockPos) {
        BlockState blockstate = reader.getBlockState(blockPos);
        FluidState fluidstate = reader.getFluidState(blockPos);
        Vec3 vector3d = context.getFrom();
        Vec3 vector3d1 = context.getTo();
        VoxelShape voxelshape = context.getBlockShape(blockstate, reader, blockPos);
        BlockHitResult blockraytraceresult = reader.clipWithInteractionOverride(vector3d, vector3d1, blockPos, voxelshape, blockstate);
        VoxelShape voxelshape1 = context.getFluidShape(fluidstate, reader, blockPos);
        BlockHitResult blockraytraceresult1 = voxelshape1.clip(vector3d, vector3d1, blockPos);
        double d0 = blockraytraceresult == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(blockraytraceresult.getLocation());
        double d1 = blockraytraceresult1 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(blockraytraceresult1.getLocation());
        return d0 <= d1 ? blockraytraceresult : blockraytraceresult1;
    }

    private static boolean doRayTrace(Vec3 from, Vec3 to, Function<BlockPos, BlockHitResult> rayTracer, BlockPos search) {
        double d = Mth.lerp(-1.0E-7, to.x, from.x);
        double e = Mth.lerp(-1.0E-7, to.y, from.y);
        double f = Mth.lerp(-1.0E-7, to.z, from.z);
        double g = Mth.lerp(-1.0E-7, from.x, to.x);
        double h = Mth.lerp(-1.0E-7, from.y, to.y);
        double i = Mth.lerp(-1.0E-7, from.z, to.z);
        int j = Mth.floor(g);
        int k = Mth.floor(h);
        int l = Mth.floor(i);
        BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos(j, k, l);
        BlockHitResult blockraytraceresult = (BlockHitResult) rayTracer.apply(blockpos$mutable);
        if (blockraytraceresult != null) {
            return false;
        } else if (blockpos$mutable.equals(search)) {
            return true;
        } else {
            double d6 = d - g;
            double d7 = e - h;
            double d8 = f - i;
            int p = Mth.sign(d6);
            int i1 = Mth.sign(d7);
            int j1 = Mth.sign(d8);
            double d9 = p == 0 ? Double.MAX_VALUE : (double)p / d6;
            double d10 = i1 == 0 ? Double.MAX_VALUE : (double)i1 / d7;
            double d11 = j1 == 0 ? Double.MAX_VALUE : (double)j1 / d8;
            double d12 = d9 * (p > 0 ? 1.0 - Mth.frac(g) : Mth.frac(g));
            double d13 = d10 * (i1 > 0 ? 1.0 - Mth.frac(h) : Mth.frac(h));
            double d14 = d11 * (j1 > 0 ? 1.0 - Mth.frac(i) : Mth.frac(i));

            do {
                if (!(d12 <= 1.0) && !(d13 <= 1.0) && !(d14 <= 1.0)) {
                    return false;
                }

                if (d12 < d13) {
                    if (d12 < d14) {
                        j += p;
                        d12 += d9;
                    } else {
                        l += j1;
                        d14 += d11;
                    }
                } else if (d13 < d14) {
                    k += i1;
                    d13 += d10;
                } else {
                    l += j1;
                    d14 += d11;
                }

                blockraytraceresult = (BlockHitResult) rayTracer.apply(blockpos$mutable.set(j, k, l));
                if (blockraytraceresult != null) {
                    return false;
                }
            } while(!blockpos$mutable.equals(search));

            return true;
        }
    }
}
