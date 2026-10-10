package dev.slarrties.privit.client.render.pipeline;

import dev.slarrties.privit.client.render.adapter.*;
import dev.slarrties.privit.client.render.compat.IrisCompat;
import dev.slarrties.privit.client.render.face.FaceLayers;
import dev.slarrties.privit.client.render.face.FaceMesh;
import dev.slarrties.privit.client.render.face.FaceTintCache;
import dev.slarrties.privit.client.render.edge.EdgeTintCache;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;
import dev.slarrties.privit.client.render.state.RegionRenderCache;
import dev.slarrties.privit.client.render.config.IrisRenderProfile;
import dev.slarrties.privit.client.render.config.VanillaRenderProfile;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

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

    private final RenderAdapter vanillaAdapter;
    private final RenderAdapter irisAdapter;
    private final boolean irisAvailable;

    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 120_000L;
    private static final long MAX_INACTIVE_TIME_MS = 120_000L;

    private RegionRenderSystem() {
        this.cache = new RegionRenderCache();
        this.faceTintCache = new FaceTintCache();
        this.edgeTintCache = new EdgeTintCache();
        this.faceLayers = new FaceLayers();
        this.vanillaAdapter = new VanillaAdapter(new VanillaRenderProfile(), faceTintCache, edgeTintCache, faceLayers);

        boolean irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
        if (irisLoaded) {
            this.irisAdapter = new IrisAdapter(new IrisRenderProfile(), faceTintCache, edgeTintCache, faceLayers);
        } else {
            this.irisAdapter = null;
        }

        this.irisAvailable = irisLoaded;
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RegionRenderSystem::renderFaces);
        WorldRenderEvents.LAST.register(RegionRenderSystem::renderEdges);
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
        if (irisAvailable && isIrisShaderActive()) return irisAdapter;

        return vanillaAdapter;
    }

    private static boolean isIrisShaderActive() {
        try {
            return IrisCompat.isShaderPackInUse();
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    private static void renderFaces(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        INSTANCE.cleanupIfNeeded();

        Vec3d cameraPos = context.camera().getPos();
        Frustum frustum = context.frustum();
        List<FaceMesh> visible = new ArrayList<>();

        for (FaceMesh mesh : INSTANCE.cache.getCachedMeshes()) {
            if (mesh.isEmpty()) continue;
            Box bounds = mesh.boundingBox();
            if (RenderCulling.isFrustumVisible(frustum, bounds) && RenderCulling.isWithinRenderDistance(cameraPos, bounds)) visible.add(mesh);
        }
        if (visible.isEmpty()) return;

        visible.sort(Comparator.comparingDouble((FaceMesh m) -> cameraPos.squaredDistanceTo(m.boundingBox().getCenter())).reversed());

        var matrices = context.matrixStack();
        matrices.push();

        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            INSTANCE.getActiveAdapter().renderFaces(visible, context);
        } finally {
            matrices.pop();
        }
    }

    private static void renderEdges(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Vec3d cameraPos = context.camera().getPos();
        Frustum frustum = context.frustum();
        List<RegionRenderEntry> visible = new ArrayList<>();

        for (RegionRenderEntry entry : INSTANCE.cache.getActiveEntries()) {
            Box bounds = entry.boundingBox();

            if (RenderCulling.isFrustumVisible(frustum, bounds) && RenderCulling.isWithinRenderDistance(cameraPos, bounds)) {
                visible.add(entry);
            }
        }
        if (visible.isEmpty()) return;

        var matrices = context.matrixStack();
        matrices.push();

        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            INSTANCE.getActiveAdapter().renderEdges(visible, context);
        } finally {
            matrices.pop();
        }
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();

        if (now - lastCleanupTime >= CLEANUP_INTERVAL_MS) {
            lastCleanupTime = now;
            cache.cleanupInactive(MAX_INACTIVE_TIME_MS);
        }
    }
}