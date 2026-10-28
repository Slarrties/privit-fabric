package dev.slarrties.privit.client.render.pipeline;

import dev.slarrties.privit.client.render.adapter.*;
import dev.slarrties.privit.client.render.config.RenderConfig;
import dev.slarrties.privit.client.render.face.FaceMesh;
import dev.slarrties.privit.client.render.face.FaceLayers;
import dev.slarrties.privit.client.render.face.FaceTintCache;
import dev.slarrties.privit.client.render.edge.EdgeLayers;
import dev.slarrties.privit.client.render.edge.EdgeTintCache;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;
import dev.slarrties.privit.client.render.state.RegionRenderCache;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;

public final class RegionRenderSystem {
    private static final RegionRenderSystem INSTANCE = new RegionRenderSystem();

    private final RegionRenderCache cache;
    private final FaceTintCache faceTintCache;
    private final EdgeTintCache edgeTintCache;
    private final FaceLayers faceLayers;
    private final RenderAdapter renderAdapter;

    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 120_000L;
    private static final long MAX_INACTIVE_TIME_MS = 120_000L;

    private RegionRenderSystem() {
        this.cache = new RegionRenderCache();
        this.faceTintCache = new FaceTintCache();
        this.edgeTintCache = new EdgeTintCache();
        this.faceLayers = new FaceLayers();
        this.renderAdapter = new RenderAdapter(new RenderConfig(), faceTintCache, edgeTintCache, faceLayers);
    }

    public static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(RegionRenderSystem::renderFaces);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(RegionRenderSystem::renderEdges);
    }

    public static void updateOrMerge(RegionRenderEntry entry) { INSTANCE.cache.updateOrMerge(entry); }
    public static void setGridVisible(UUID regionId, boolean visible) { INSTANCE.cache.setGridVisible(regionId, visible); }
    public static boolean isGridVisible(UUID regionId) { return INSTANCE.cache.isGridVisible(regionId); }
    public static void clearAll() { INSTANCE.cache.clear(); }
    public static void removeRegion(UUID regionId) { if (regionId != null) INSTANCE.cache.remove(regionId); }

    public static void disableAndRemove(UUID regionId) {
        if (regionId != null) {
            INSTANCE.cache.setGridVisible(regionId, false);
            INSTANCE.cache.remove(regionId);
        }
    }

    private RenderAdapter getActiveAdapter() {
        return renderAdapter;
    }

    private static void renderFaces(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        INSTANCE.cleanupIfNeeded();

        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
        Frustum frustum = client.gameRenderer.getMainCamera().getCullFrustum();

        List<FaceMesh> visible = new ArrayList<>();
        for (FaceMesh mesh : INSTANCE.cache.getCachedMeshes()) {
            if (mesh.isEmpty()) continue;
            AABB bounds = mesh.boundingBox();
            if (RenderCulling.isFrustumVisible(frustum, bounds)
                    && RenderCulling.isWithinRenderDistance(cameraPos, bounds)) {
                visible.add(mesh);
            }
        }

        if (visible.isEmpty()) return;
        visible.sort(Comparator
                .comparingDouble((FaceMesh m) -> cameraPos.distanceToSqr(m.boundingBox().getCenter()))
                .reversed());

        var matrices = context.poseStack();
        matrices.pushPose();
        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            INSTANCE.getActiveAdapter().renderFaces(visible, context);
        } finally {
            matrices.popPose();
        }
    }

    private static void renderEdges(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
        Frustum frustum = client.gameRenderer.getMainCamera().getCullFrustum();

        List<RegionRenderEntry> visible = new ArrayList<>();
        for (RegionRenderEntry entry : INSTANCE.cache.getActiveEntries()) {
            AABB bounds = entry.boundingBox();
            if (RenderCulling.isFrustumVisible(frustum, bounds)
                    && RenderCulling.isWithinRenderDistance(cameraPos, bounds)) {
                visible.add(entry);
            }
        }

        if (visible.isEmpty()) return;

        RenderType edgeLayer = EdgeLayers.EDGE_QUADS;

        var matrices = context.poseStack();
        matrices.pushPose();
        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            INSTANCE.getActiveAdapter().renderEdges(visible, context, edgeLayer);
        } finally {
            matrices.popPose();
        }

        context.bufferSource().endBatch(edgeLayer);
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime >= CLEANUP_INTERVAL_MS) {
            lastCleanupTime = now;
            cache.cleanupInactive(MAX_INACTIVE_TIME_MS);
        }
    }
}