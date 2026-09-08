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
    private static final double BLOCK_EPSILON = 0.015D;
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

        // Matrices used by Minecraft's renderer is obtained.
        PoseStack poseStack = context.matrixStack();

        // Minecraft's buffer source is obtained.
        MultiBufferSource consumers = context.consumers();
        if (poseStack == null || consumers == null || blueprint == null || origin == null) {
            return;
        }

        // NOTE: renderLineBox() requires a VertexConsumer, thus the appropriate buffer is obtained via MultiBufferSource.
        VertexConsumer lines = consumers.getBuffer(net.minecraft.client.renderer.RenderType.lines());
        BlocksRenderer(poseStack, lines);
        BoundsRenderer(poseStack, lines, blueprint);
    }

        private static void BlocksRenderer(PoseStack poseStack, VertexConsumer lines) {
            BlockPos[] positions = PreviewManager.getPreviewPositions();
            for(BlockPos local:  positions) {
                BlockPos world = PreviewManager.transformPosition(local);
                AABB box = new AABB(world.getX() + BLOCK_EPSILON, world.getY() + BLOCK_EPSILON,
                        world.getZ() + BLOCK_EPSILON, world.getX() + 1.0D - BLOCK_EPSILON,
                        world.getY() + 1.0D - BLOCK_EPSILON, world.getZ() + 1.0D - BLOCK_EPSILON);
                LevelRenderer.renderLineBox(poseStack, lines, box, 0.25F, 0.85F, 1.0F, 0.70F);
            }
        }

        private static void BoundsRenderer(PoseStack poseStack, VertexConsumer lines, Blueprint blueprint) {
            BlockPos[] corners = {
                    PreviewManager.transformPosition(new BlockPos(0, 0, 0)),
                    PreviewManager.transformPosition(new BlockPos(blueprint.getWidth(), 0, 0)),
                    PreviewManager.transformPosition(new BlockPos(0, blueprint.getHeight(), 0)),
                    PreviewManager.transformPosition(new BlockPos(0, 0, blueprint.getDepth())),
            };
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for(BlockPos corner: corners) {
                minX = Math.min(minX, corner.getX());
                minY = Math.min(minY, corner.getY());
                minZ = Math.min(minZ, corner.getZ());
                maxX = Math.max(maxX, corner.getX());
                maxY = Math.max(maxY, corner.getY());
                maxZ = Math.max(maxZ, corner.getZ());
            }
            AABB bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            // NOTE FOR COLLABORATORS: The basic world-space line rendering infrastructure is provided by
            // Minecraft's Level Renderer. As a result, the call itself may need to be adapted to the available
            // WorldRenderContext API depending on the exact Fabric API mappings in the project's current dependency set.

            // Bounding Box is drawn. Final 4 values are Red, Green, Blue & Alpha.
            // These are all currently 1.0.
            LevelRenderer.renderLineBox(poseStack, lines, bounds, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
