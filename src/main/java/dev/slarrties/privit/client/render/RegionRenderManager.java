package dev.slarrties.privit.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.render.animation.AnimationRegistry;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockBox;
import net.minecraft.client.render.*;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import org.joml.Matrix4f;

import java.util.List;
import java.util.UUID;

public final class RegionRenderManager {

    private static final RegionRenderer RENDERER = new RegionRenderer();
    private static final int CLEANUP_INTERVAL_TICKS = 600; // every 30 sec
    private static RegionRenderManager INSTANCE;
    private int tickCounter = 0;


    private static RegionRenderManager getInstance() {
        if (INSTANCE == null) INSTANCE = new RegionRenderManager();

        return INSTANCE;
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RegionRenderManager::renderFaces);
        WorldRenderEvents.LAST.register(RegionRenderManager::renderEdges);
    }

    private static void renderFaces(WorldRenderContext context) {
        if (RegionRenderConfig.useShaderCompat()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        var consumers = context.consumers();
        if (consumers == null) return;

        var matrices = context.matrixStack();
        var cameraPos = context.camera().getPos();
        var frustum = context.frustum();

        getInstance().cleanupIfNeeded();
        matrices.push();
        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            List<RegionRenderEntry> active = RegionRenderCache.getInstance().getActiveEntries();
            List<RegionGeometry> geometries = GeometryCalculator.computeAll(active);

            for (RegionGeometry geometry : geometries) {
                if (geometry.isEmpty()) continue;
                Box bounds = getBoundingBox(geometry);
                if (frustum != null && !frustum.isVisible(bounds)) continue;
                if (isTooFar(cameraPos, bounds)) continue;
                RENDERER.render(geometry, context);
            }
        } finally {
            matrices.pop();
        }
    }

    private static void renderEdges(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        List<RegionRenderEntry> active = RegionRenderCache.getInstance().getActiveEntries();
        if (active.isEmpty()) return;

        var matrices = context.matrixStack();
        var cameraPos = context.camera().getPos();

        matrices.push();
        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            if (RegionRenderConfig.useShaderCompat()) {
                drawShaderCompat(active, matrix, cameraPos);
                return;
            }

            VertexConsumerProvider consumers = context.consumers();
            if (consumers != null) {
                RENDERER.renderEdges(active, consumers.getBuffer(RegionRenderLayers.EDGE_LAYER), matrix);
                return;
            }

            drawEdgesImmediate(active, matrix);
        } finally {
            matrices.pop();
        }
    }

    private static void drawShaderCompat(List<RegionRenderEntry> entries, Matrix4f matrix, Vec3d cameraPos) {
        List<RegionGeometry> geometries = GeometryCalculator.computeAll(entries);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        for (RegionGeometry geometry : geometries) {
            if (geometry.isEmpty()) continue;

            Identifier texture = switch (geometry.getType()) {
                case DRAFT -> AnimationRegistry.getInstance()
                        .getAnimation(RenderType.DRAFT)
                        .getCurrentFrame();
                case CONFLICT -> Identifier.of(PrivitMod.MOD_ID, "textures/region/conflict.png");
                case ORIGINAL -> Identifier.of(PrivitMod.MOD_ID, "textures/region/real.png");
            };
            RenderSystem.setShaderTexture(0, texture);

            BufferBuilder fill = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            RENDERER.renderFacesPositionColor(List.of(geometry), fill, matrix);
            BufferRenderer.drawWithGlobalProgram(fill.end());
        }

        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder edges = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderEdgesAsQuads(entries, edges, matrix, cameraPos);
        BufferRenderer.drawWithGlobalProgram(edges.end());

        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void renderEdgesAsQuads(List<RegionRenderEntry> entries, VertexConsumer buffer,
                                           Matrix4f matrix, Vec3d cameraPos) {
        float half = 0.015f;

        for (RegionRenderEntry entry : entries) {
            RenderProperties props = RenderProperties.fromColor(entry.color(), 1.0f, false);
            float r = props.edgeRed();
            float g = props.edgeGreen();
            float b = props.edgeBlue();
            float a = props.edgeAlpha();

            for (BlockBox box : entry.conflicts()) {
                emitBox(buffer, matrix, cameraPos, box, true, half, r, g, b, a);
            }
            if (entry.hasOriginal()) {
                emitBox(buffer, matrix, cameraPos, entry.original(), false, half, r, g, b, a);
            }
            if (entry.hasDraft()) {
                emitBox(buffer, matrix, cameraPos, entry.draft(), true, half, r, g, b, a);
            }
        }
    }

    private static void emitBox(VertexConsumer buffer, Matrix4f matrix, Vec3d cameraPos,
                                BlockBox box, boolean dashed, float half,
                                float r, float g, float b, float a) {
        if (box == null) return;

        double minX = box.getMinX();
        double minY = box.getMinY();
        double minZ = box.getMinZ();
        double maxX = box.getMaxX() + 1.0;
        double maxY = box.getMaxY() + 1.0;
        double maxZ = box.getMaxZ() + 1.0;

        double[][] lines = {
                {minX, minY, minZ, maxX, minY, minZ},
                {maxX, minY, minZ, maxX, minY, maxZ},
                {maxX, minY, maxZ, minX, minY, maxZ},
                {minX, minY, maxZ, minX, minY, minZ},
                {minX, maxY, minZ, maxX, maxY, minZ},
                {maxX, maxY, minZ, maxX, maxY, maxZ},
                {maxX, maxY, maxZ, minX, maxY, maxZ},
                {minX, maxY, maxZ, minX, maxY, minZ},
                {minX, minY, minZ, minX, maxY, minZ},
                {maxX, minY, minZ, maxX, maxY, minZ},
                {maxX, minY, maxZ, maxX, maxY, maxZ},
                {minX, minY, maxZ, minX, maxY, maxZ}
        };

        for (double[] e : lines) {
            if (dashed) {
                emitDashedQuad(buffer, matrix, cameraPos, e[0], e[1], e[2], e[3], e[4], e[5], half, r, g, b, a);
            } else {
                RENDERER.emitEdgeQuad(buffer, matrix, e[0], e[1], e[2], e[3], e[4], e[5], cameraPos, half, r, g, b, a);
            }
        }
    }

    private static void emitDashedQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d cameraPos,
                                       double x1, double y1, double z1,
                                       double x2, double y2, double z2,
                                       float half, float r, float g, float b, float a) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001) return;

        double dash = RegionRenderConfig.EDGE_DASH_LENGTH_BLOCKS;
        double gap = RegionRenderConfig.EDGE_DASH_GAP_BLOCKS;
        double seg = dash + gap;
        int segments = (int) Math.ceil(length / seg);

        for (int i = 0; i < segments; i++) {
            double t1 = i * seg / length;
            double t2 = Math.min((i * seg + dash) / length, 1.0);
            if (t1 >= 1.0) break;

            RENDERER.emitEdgeQuad(
                    buffer, matrix,
                    x1 + dx * t1, y1 + dy * t1, z1 + dz * t1,
                    x1 + dx * t2, y1 + dy * t2, z1 + dz * t2,
                    cameraPos, half, r, g, b, a
            );
        }
    }

    private static void drawEdgesImmediate(List<RegionRenderEntry> entries, Matrix4f matrix) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        RENDERER.renderEdges(entries, buffer, matrix);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static Box getBoundingBox(RegionGeometry geometry) {
        if (geometry.isEmpty()) {
            return new Box(0, 0, 0, 1, 1, 1);
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

        for (RegionFace face : geometry.getFaces()) {
            minX = Math.min(minX, face.minX());
            minY = Math.min(minY, face.minY());
            minZ = Math.min(minZ, face.minZ());
            maxX = Math.max(maxX, face.maxX());
            maxY = Math.max(maxY, face.maxY());
            maxZ = Math.max(maxZ, face.maxZ());
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean isTooFar(Vec3d cameraPos, Box box) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerY = (box.minY + box.maxY) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;

        double distSq = cameraPos.squaredDistanceTo(centerX, centerY, centerZ);
        return distSq > (32 * 16) * (32 * 16);
    }

    public static void updateRegion(RegionRenderEntry entry) { RegionRenderCache.getInstance().updateEntry(entry); }

    public static void setGridVisible(UUID regionId, boolean visible) { RegionRenderCache.getInstance().setGridVisible(regionId, visible); }

    public static boolean isGridVisible(UUID regionId) {
        RegionRenderEntry entry = RegionRenderCache.getInstance().getEntryIfPresent(regionId);
        return entry != null && entry.isGridVisible();
    }

    // =====================================================================
    // Cleanup
    // =====================================================================

    public static void clearAll() { RegionRenderCache.getInstance().clear(); }

    private void cleanupIfNeeded() {
        tickCounter++;

        if (tickCounter >= CLEANUP_INTERVAL_TICKS) {
            tickCounter = 0;
            RegionRenderCache.getInstance().cleanupInactive(60_000);
        }
    }

    public static void removeRegion(UUID regionId) {
        if (regionId != null) {
            RegionRenderCache.getInstance().remove(regionId);
        }
    }

    public static void disableAndRemove(UUID regionId) {
        if (regionId != null) {
            RegionRenderCache.getInstance().setGridVisible(regionId, false);
            RegionRenderCache.getInstance().remove(regionId);
        }
    }
}