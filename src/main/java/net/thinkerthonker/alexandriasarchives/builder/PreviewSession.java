package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import java.util.Objects;

// Server-side checks for pending blueprint preview of player. This class does not change or replace the blueprint
// after the preview starts but permits changes to the world origin and rotation.

public class PreviewSession {
    private static final int ROTATION_COUNT = 4;
    private final Blueprint blueprint;
    private BlockPos origin;
    private int rotation;
    public PreviewSession(Blueprint blueprint, BlockPos origin) {
        this.blueprint = Objects.requireNonNull(blueprint, "blueprint");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.rotation = 0;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public int getRotation() {
        return rotation;
    }

    public void setOrigin(BlockPos origin) {
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    public void setRotation(int rotation) {
        this.rotation = Math.floorMod(rotation, ROTATION_COUNT);
    }

    public void rotateClockwise() {
        rotation = Math.floorMod(rotation + 1, ROTATION_COUNT);
    }
}
