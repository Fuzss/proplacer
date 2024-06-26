package fuzs.proplacer.client.handler;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFastBlockHandler {
    @Nullable
    protected BlockPos blockPos;
    @Nullable
    private BlockPos newBlockPos;
    @Nullable
    protected Direction direction;

    public final void onStartClientTick(Minecraft minecraft) {

        // needs to run at the beginning of client tick to avoid double placing in a single tick when vanilla has just placed a block
        if (!KeyBindingHandler.isFastPlacementActive() || !this.getKeyMapping(minecraft.options).isDown()) {

            this.clear();
        } else {

            this.tickNewPosition(minecraft.level);
            if (this.isActive()) {

                this.tickWhenActive(minecraft);
            } else {

                this.tickNonActive(minecraft);
            }
        }
    }

    protected abstract void tickNonActive(Minecraft minecraft);

    protected abstract void tickWhenActive(Minecraft minecraft);

    protected void setNewBlockPos(BlockPos newBlockPos) {
        this.newBlockPos = newBlockPos;
    }

    protected BlockPos getTargetPosition() {
        return this.blockPos.relative(this.direction);
    }

    public boolean isActive() {
        return this.direction != null;
    }

    public void clear() {
        this.blockPos = null;
        this.direction = null;
    }

    private void tickNewPosition(Level level) {

        if (this.newBlockPos != null) {

            // prevent setting block position to the next one when the last breaking attempt was unsuccessful
            if (this.requireEmptyBlock() == level.isEmptyBlock(this.newBlockPos)) {

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

    protected abstract KeyMapping getKeyMapping(Options options);

    protected abstract boolean requireEmptyBlock();
}
