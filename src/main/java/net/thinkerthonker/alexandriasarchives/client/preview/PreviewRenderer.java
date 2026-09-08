package net.thinkerthonker.alexandriasarchives.client.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;

// Alexandria's Archives is rendered under a construction preview in this class.
// The class renders a bounding box around the blueprint without having the preview place blocks.
public final class PreviewRenderer {
    private static final double BLOCK_EPSILON = 0.015D;

    // Above this many blocks, per-block outlines are dropped in favour of
    // the bounding box and footprint alone.
    private static final int MAX_OUTLINED_BLOCKS = 6000;
    private PreviewRenderer() {
        // Utility Class.
    }

    // Preview renderer is registered.
    public static void initialize() {
        // AFTER_ENTITIES rather than AFTER_TRANSLUCENT: the line buffer is
        // flushed as part of the entity batch, so vertices submitted after
        // that point are silently discarded and nothing appears.
        WorldRenderEvents.AFTER_ENTITIES.register(PreviewRenderer::render);
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

        // renderLineBox() draws in camera space, not world space. Without
        // this translation every box is offset by the camera's own position,
        // which puts the preview nowhere near the ground it belongs to.
        Camera camera = context.camera();
        if (camera == null) {
            return;
        }
        Vec3 view = camera.getPosition();

        // NOTE: renderLineBox() requires a VertexConsumer, thus the appropriate buffer is obtained via MultiBufferSource.
        VertexConsumer lines = consumers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-view.x, -view.y, -view.z);

        // Outlining every block is only affordable on small structures. A
        // landmark can carry hundreds of thousands of them (the 1:1 Burj
        // Khalifa is 326,630), and one line box each would submit millions
        // of vertices per frame. Past the threshold the shape is conveyed by
        // the bounding box and a footprint on the ground instead.
        if (blueprint.getBlockCount() <= MAX_OUTLINED_BLOCKS) {
            BlocksRenderer(poseStack, lines);
        }
        FootprintRenderer(poseStack, lines, blueprint);
        BoundsRenderer(poseStack, lines, blueprint);

        poseStack.popPose();

        // Force the batch out now; leaving it to the caller means it can be
        // dropped depending on where in the frame this event landed.
        if (consumers instanceof BufferSource bufferSource) {
            bufferSource.endBatch(RenderType.lines());
        }
    }

    // The ground plan of the structure, drawn at the base of the bounding
    // box. This is the part that answers "where will it actually land?" -
    // the full box is often far overhead and hard to read against the sky.
    private static void FootprintRenderer(PoseStack poseStack, VertexConsumer lines, Blueprint blueprint) {
        AABB bounds = worldBounds(blueprint);
        AABB footprint = new AABB(
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.minY + 0.05D, bounds.maxZ);
        LevelRenderer.renderLineBox(poseStack, lines, footprint, 1.0F, 0.62F, 0.0F, 0.9F);

        // A short riser at each corner, so the footprint still reads when
        // viewed from ground level rather than from above.
        double riser = Math.min(6.0D, Math.max(2.0D, bounds.getYsize() * 0.05D));
        double[][] corners = {
                {bounds.minX, bounds.minZ}, {bounds.maxX, bounds.minZ},
                {bounds.minX, bounds.maxZ}, {bounds.maxX, bounds.maxZ},
        };
        for (double[] corner : corners) {
            AABB post = new AABB(
                    corner[0] - 0.06D, bounds.minY, corner[1] - 0.06D,
                    corner[0] + 0.06D, bounds.minY + riser, corner[1] + 0.06D);
            LevelRenderer.renderLineBox(poseStack, lines, post, 1.0F, 0.62F, 0.0F, 0.85F);
        }
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

        private static AABB worldBounds(Blueprint blueprint) {
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
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        // NOTE FOR COLLABORATORS: The basic world-space line rendering infrastructure is provided by
        // Minecraft's Level Renderer. As a result, the call itself may need to be adapted to the available
        // WorldRenderContext API depending on the exact Fabric API mappings in the project's current dependency set.
        private static void BoundsRenderer(PoseStack poseStack, VertexConsumer lines, Blueprint blueprint) {
            // Bounding Box is drawn. Final 4 values are Red, Green, Blue & Alpha.
            LevelRenderer.renderLineBox(poseStack, lines, worldBounds(blueprint), 1.0F, 1.0F, 1.0F, 0.85F);
    }
}
