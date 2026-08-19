package net.thinkerthonker.alexandriasarchives.client.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

// This class controls the client-side preview of one Alexandria's Archives blueprint without
// modifying the minecraft world. It stores the previewed blueprint, the preview's origin,
// the rotation and the active status of the preview mode.
public final class PreviewManager {
    public static Blueprint blueprint;
    public static BlockPos origin;
    public static int rotation = 0;
    public static boolean active = false;
    private PreviewManager() {
        // Utility Class.
    }

    // Preview system is initialized.
    public static void initialize() {
        blueprint = null;
        origin = null;
        rotation = 0;
        active = false;
    }

    // Calls every client tick.
    public static void tick(Minecraft client) {
        if(!active || blueprint == null || origin == null) {
            return;
        }

        // NOTE FOR COLLABORATORS: Movement Controls TBA
        // Operations are to only modify the preview position.
    }

    // Blueprint added to preview layer.
    // @param blueprint preview from blueprint.
    // @param origin initial world position.
    public static void startPreview(Blueprint blueprint, BlockPos origin) {
        PreviewManager.blueprint = blueprint;
        PreviewManager.origin = origin;
        PreviewManager.rotation = 0;
        PreviewManager.active = true;
    }

    // Preview is stopped.
    public static void stopPreview() {
        blueprint = null;
        origin = null;
        rotation = 0;
        active = false;
    }

    // Returns active status of preview.
    public static boolean isActive() {
        return active;
    }

    // Returns blueprint currently in preview.
    public static Blueprint getBlueprint() {
        return blueprint;
    }

    // Returns origin of current preview.
    public static BlockPos getOrigin() {
        return origin;
    }

    // Moves preview.
    public static void move(int x, int y, int z) {
        if(!active || origin == null) {
            return;
        }
        origin = origin.offset(x, y, z);
    }

    // Preview is rotated 90-degrees clockwise.
    public static void rotateClockwise() {
        if(!active) {
            return;
        }
        rotation = (rotation + 1) % 4;
    }

    // Returns current rotation.
    public static int getRotation() {
        return rotation;
    }

    // Blueprint-local position is converted into world position.
    // Performs rotation around Y-axis.
    public static BlockPos transformPosition(BlockPos localPosition) {
        if(origin == null) {
            return localPosition;
        }
        int x = localPosition.getX();
        int y = localPosition.getY();
        int z = localPosition.getZ();
        BlockPos rotated;
        switch(rotation) {
            case 0:
                rotated = new BlockPos(-z, y, x);
                break;
            case 1:
                rotated = new BlockPos(-x, -y, -z);
                break;
            case 2:
                rotated = new BlockPos(z, y, -x);
                break;
            default:
                rotated = localPosition;
        }
        return origin.offset(rotated);
    }
}
