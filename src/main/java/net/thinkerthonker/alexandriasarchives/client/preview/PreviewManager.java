package net.thinkerthonker.alexandriasarchives.client.preview;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.Input;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import net.thinkerthonker.alexandriasarchives.network.PreviewPayload;
import net.thinkerthonker.alexandriasarchives.network.PreviewTransformPayload;
import org.lwjgl.glfw.GLFW;
import java.util.List;

// This class controls the client-side preview of one Alexandria's Archives blueprint without
// modifying the minecraft world. It stores the previewed blueprint, the preview's origin,
// the rotation and the active status of the preview mode.
public final class PreviewManager {
    private static final int ROTATION_COUNT = 4;
    private static final int MOVE_INTERVAL_TICKS = 3;
    public static Blueprint blueprint;
    public static BlockPos origin;
    public static int rotation;
    public static boolean active;
    private static BlockPos[] previewPositions = new BlockPos[0];
    private static String[] previewBlockIDs = new String[0];
    private static boolean previousR;
    private static int movementCooldown;
    private PreviewManager() {
        // Utility Class.
    }

    // Preview system is initialized.
    public static void initialize() {
        stopPreview();
        previousR = false;
        movementCooldown = 0;
    }

    // Calls every client tick.
    public static void tick(Minecraft client) {
        if(!active || blueprint == null || origin == null) {
            return;
        }
        if(movementCooldown > 0) {
            movementCooldown--;
        }
        long window = client.getWindow().getWindow();
        boolean rPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_R);
        if(rPressed && !previousR) {
            rotateClockwise();
        }
        previousR = rPressed;
        if(movementCooldown > 0) {
            return;
        }
        int x = 0;
        int y = 0;
        int z = 0;
        if(isDown(window, GLFW.GLFW_KEY_UP)) {
            z--;
        }
        if(isDown(window, GLFW.GLFW_KEY_DOWN)) {
            z++;
        }
        if(isDown(window, GLFW.GLFW_KEY_LEFT)) {
            x--;
        }
        if(isDown(window, GLFW.GLFW_KEY_RIGHT)) {
            x++;
        }
        if(isDown(window, GLFW.GLFW_KEY_PAGE_UP)) {
            y++;
        }
        if(isDown(window, GLFW.GLFW_KEY_PAGE_DOWN)) {
            y--;
        }
        if(x != 0 || y != 0 || z != 0) {
            move(x, y, z);
            movementCooldown = MOVE_INTERVAL_TICKS;
        }
        // NOTE FOR COLLABORATORS: Movement Controls TBA
        // Operations are to only modify the preview position.
    }

    private static boolean isDown(long window, int keyCode) {
        return InputConstants.isKeyDown(window, keyCode);
    }
    public static void applyPayload(PreviewPayload payload) {
        if(!payload.active()) {
            stopPreview();
            return;
        }
        blueprint = createBlueprint(payload);
        origin = payload.origin();
        rotation = Math.floorMod(payload.rotation(), ROTATION_COUNT);
        previewPositions = payload.blockPositions().clone();
        previewBlockIDs = payload.blockIDs().clone();
        active = true;
    }

    private static Blueprint createBlueprint(PreviewPayload payload) {
        List<BlueprintBlock> blocks = new java.util.ArrayList<>(payload.blockPositions().length);
        for(int i = 0; i < payload.blockPositions().length; i++) {
            blocks.add(new BlueprintBlock(payload.blockPositions()[i], payload.blockIDs()[i]));
        }
        return new Blueprint(payload.blueprintID(), payload.blueprintName(), payload.width(), payload.height(),
                payload.depth(), blocks);
    }

    // Blueprint added to preview layer.
    // @param blueprint preview from blueprint.
    // @param origin initial world position.
    public static void startPreview(Blueprint blueprint, BlockPos origin) {
        PreviewManager.blueprint = blueprint;
        PreviewManager.origin = origin;
        PreviewManager.rotation = 0;
        PreviewManager.active = true;
        List<BlueprintBlock> blocks = blueprint.getBlocks();
        previewPositions = new BlockPos[blocks.size()];
        previewBlockIDs = new String[blocks.size()];
        for(int i = 0; i < blocks.size(); i++) {
            BlueprintBlock block = blocks.get(i);
            previewPositions[i] = block.getPosition();
            previewBlockIDs[i] = block.getBlockID();
        }
    }

    // Preview is stopped.
    public static void stopPreview() {
        blueprint = null;
        origin = null;
        rotation = 0;
        active = false;
        previewPositions = new BlockPos[0];
        previewBlockIDs = new String[0];
        movementCooldown = 0;
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
        synchronizeTransform();
    }

    // Preview is rotated 90-degrees clockwise.
    public static void rotateClockwise() {
        if(!active) {
            return;
        }
        rotation = Math.floorMod(rotation + 1, ROTATION_COUNT);
        synchronizeTransform();
    }

    public static void synchronizeTransform() {
        if(!active || origin == null) {
            return;
        }
        ClientPlayNetworking.send(new PreviewTransformPayload(origin, rotation));
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

    public static String[] getPreviewBlockIDs() {
        return previewBlockIDs;
    }

    public static BlockPos[] getPreviewPositions() {
        return previewPositions;
    }
}
