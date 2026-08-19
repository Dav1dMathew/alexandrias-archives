package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;

public class PreviewSession {
    private final Blueprint blueprint;
    private BlockPos origin;
    private int rotation;
    public PreviewSession(Blueprint blueprint, BlockPos origin) {
        this.blueprint = blueprint;
        this.origin = origin;
        this.rotation = 0;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public void setOrigin(BlockPos origin) {
        this.origin = origin;
    }

    public int getRotation() {
        return rotation;
    }

    public void rotate() {
        rotation = (rotation + 1) % 4;
    }
}
