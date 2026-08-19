package net.thinkerthonker.alexandriasarchives.client.preview;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import net.thinkerthonker.alexandriasarchives.network.PreviewPayload;
import net.thinkerthonker.alexandriasarchives.network.PreviewTransformPayload;
import org.lwjgl.glfw.GLFW;

// This class controls the client-side preview of one Alexandria's Archives blueprint without
// modifying the minecraft world. It stores the previewed blueprint, the preview's origin,
// the rotation and the active status of the preview mode.
public final class PreviewManager {
    public static Blueprint blueprint;
    public static BlockPos origin;
    public static int rotation = 0;
    public static boolean active = false;
    private static BlockPos[] previewPositions = new BlockPos[0];
    private static String[] previewBlockIds = new String[0];
    private static boolean previousR;
    private PreviewManager() {
        // Utility Class.
    }

    // Preview system is initialized.
    public static void initialize() {
        blueprint = null;
        origin = null;
        rotation = 0;
        active = false;
        previewPositions = new BlockPos[0];
        previewBlockIds = new String[0];
        previousR = false;
    }

    // Calls every client tick.
    public static void tick(Minecraft client) {
        if(!active || blueprint == null || origin == null || client.getWindow() == null) {
            return;
        }

        long window = client.getWindow().getWindow();
        boolean rPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_R);
        if (rPressed && !previousR) {
            rotateClockwise();
        }
        previousR = rPressed;

        // Arrow keys move the local preview and synchronize the transform with the server.
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP)) {
            move(0, 0, -1);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN)) {
            move(0, 0, 1);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT)) {
            move(-1, 0, 0);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT)) {
            move(1, 0, 0);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_PAGE_UP)) {
            move(0, 1, 0);
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_PAGE_DOWN)) {
            move(0, -1, 0);
        }
    }

    public static void applyPayload(PreviewPayload payload) {
        if (!payload.active()) {
            stopPreview();
            return;
        }

        blueprint = createBlueprint(payload);
        origin = payload.origin();
        rotation = Math.floorMod(payload.rotation(), 4);
        previewPositions = payload.blockPositions().clone();
        previewBlockIds = payload.blockIds().clone();
        active = true;
    }

    private static Blueprint createBlueprint(PreviewPayload payload) {
        java.util.List<BlueprintBlock> blocks = new java.util.ArrayList<>();
        for (int i = 0; i < payload.blockPositions().length; i++) {
            blocks.add(new BlueprintBlock(payload.blockPositions()[i], payload.blockIds()[i]));
        }
        return new Blueprint(
                payload.blueprintId(),
                payload.blueprintName(),
                payload.width(),
                payload.height(),
                payload.depth(),
                blocks
        );
    }

    public static BlockPos[] getPreviewPositions() {
        return previewPositions;
    }

    public static String[] getPreviewBlockIds() {
        return previewBlockIds;
    }

    // Blueprint added to preview layer.
    // @param blueprint preview from blueprint.
    // @param origin initial world position.
    public static void startPreview(Blueprint blueprint, BlockPos origin) {
        PreviewManager.blueprint = blueprint;
        PreviewManager.origin = origin;
        PreviewManager.rotation = 0;
        PreviewManager.active = true;
        java.util.List<BlueprintBlock> blocks = blueprint.getBlocks();
        previewPositions = new BlockPos[blocks.size()];
        previewBlockIds = new String[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            previewPositions[i] = blocks.get(i).getPosition();
            previewBlockIds[i] = blocks.get(i).getBlockID();
        }
    }

    // Preview is stopped.
    public static void stopPreview() {
        blueprint = null;
        origin = null;
        rotation = 0;
        active = false;
        previewPositions = new BlockPos[0];
        previewBlockIds = new String[0];
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

    private static void synchronizeTransform() {
        if (!active || origin == null) {
            return;
        }
        if (ClientPlayNetworking.canSend(PreviewTransformPayload.TYPE)) {
            ClientPlayNetworking.send(new PreviewTransformPayload(origin, rotation));
        }
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
        rotation = (rotation + 1) % 4;
        synchronizeTransform();
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
                rotated = new BlockPos(x, y, z);
                break;
            case 1:
                rotated = new BlockPos(-z, y, x);
                break;
            case 2:
                rotated = new BlockPos(-x, y, -z);
                break;
            default:
                rotated = new BlockPos(z, y, -x);
        }
        return origin.offset(rotated);
    }
}
