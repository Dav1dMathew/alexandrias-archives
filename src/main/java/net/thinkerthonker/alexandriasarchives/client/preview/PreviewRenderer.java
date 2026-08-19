package net.thinkerthonker.alexandriasarchives.client.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;

// Alexandria's Archives is rendered under a construction preview in this class.
// The class renders a bounding box around the blueprint without having the preview place blocks.
public final class PreviewRenderer {
    private PreviewRenderer() {
        // Utility Class.
    }

    // Preview renderer is registered.
    public static void initialize() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(PreviewRenderer::render);
    }

    // Called when world rendering occurs.
    private static void render(WorldRenderContext context) {
        if (!PreviewManager.isActive()) {
            return;
        }

        Blueprint blueprint = PreviewManager.getBlueprint();
        BlockPos origin = PreviewManager.getOrigin();
        if (blueprint == null || origin == null) {
            return;
        }

        // Matrices used by Minecraft's renderer is obtained.
        PoseStack poseStack = context.matrixStack();

        // Minecraft's buffer source is obtained.
        MultiBufferSource bufferSource = context.consumers();
        if(poseStack == null || bufferSource == null) {
            return;
        }

        // NOTE: renderLineBox() requires a VertexConsumer, thus the appropriate buffer is obtained via MultiBufferSource.
        VertexConsumer vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.lines());

        // Draw every blueprint block as a wireframe ghost so the player can see the
        // exact footprint of the proposed structure without modifying the world.
        BlockPos[] positions = PreviewManager.getPreviewPositions();
        for (BlockPos localPosition : positions) {
            BlockPos worldPosition = PreviewManager.transformPosition(localPosition);
            AABB blockBox = new AABB(
                    worldPosition.getX() + 0.02D,
                    worldPosition.getY() + 0.02D,
                    worldPosition.getZ() + 0.02D,
                    worldPosition.getX() + 0.98D,
                    worldPosition.getY() + 0.98D,
                    worldPosition.getZ() + 0.98D
            );
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, blockBox, 0.25F, 0.85F, 1.0F, 0.75F);
        }

        // Bounding Box is created.
        BlockPos first = PreviewManager.transformPosition(new BlockPos(0, 0, 0));
        BlockPos second = PreviewManager.transformPosition(new BlockPos(
                blueprint.getWidth(), blueprint.getHeight(), blueprint.getDepth()
        ));
        double minX = Math.min(first.getX(), second.getX());
        double minY = Math.min(first.getY(), second.getY());
        double minZ = Math.min(first.getZ(), second.getZ());
        double maxX = Math.max(first.getX(), second.getX());
        double maxY = Math.max(first.getY(), second.getY());
        double maxZ = Math.max(first.getZ(), second.getZ());

        AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        // NOTE FOR COLLABORATORS: The basic world-space line rendering infrastructure is provided by
        // Minecraft's Level Renderer. As a result, the call itself may need to be adapted to the available
        // WorldRenderContext API depending on the exact Fabric API mappings in the project's current dependency set.

        // Bounding Box is drawn. Final 4 values are Red, Green, Blue & Alpha.
        // These are all currently 1.0.
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
