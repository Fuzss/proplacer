package com.fuzs.proplacer.util;

import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.function.Function;

public class SimplePlayerTracker {

    private BlockRayTraceResult objectMouseOver;
    private Vector3d playerPos;

    public SimplePlayerTracker() {

        this.reset();
    }

    public void reset() {

        this.objectMouseOver = null;
        this.playerPos = Vector3d.ZERO;
    }

    public void start() {

        this.checkBlockAttribute(BlockRayTraceResult::getPos, null);
    }

    public boolean checkPosition() {

        return false;
    }

    public boolean checkMouseObject(BlockRayTraceResult objectMouseOver) {

        if (!this.checkBlockAttribute(BlockRayTraceResult::getPos, objectMouseOver)) {

            return true;
        } else {

            return !this.checkBlockAttribute(BlockRayTraceResult::getFace, objectMouseOver);
        }
    }

    private boolean checkBlockAttribute(Function<BlockRayTraceResult, ?> function, BlockRayTraceResult objectMouseOver) {

        return function.apply(this.objectMouseOver).equals(function.apply(objectMouseOver));
    }

}
