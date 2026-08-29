package dev.slarrties.privit.client.render;

import net.minecraft.util.math.Box;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

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
        WorldRenderEvents.AFTER_TRANSLUCENT.register(RegionRenderManager::renderAll);
    }

    private static void renderAll(WorldRenderContext context) {
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

            List<RegionRenderEntry> activeEntries = RegionRenderCache.getInstance().getActiveEntries();
            List<RegionGeometry> geometries = GeometryCalculator.computeAll(activeEntries);

            for (RegionGeometry geometry : geometries) {
                if (geometry.isEmpty()) continue;

                Box bounds = getBoundingBox(geometry);
                if (frustum != null && !frustum.isVisible(bounds)) continue;
                if (isTooFar(cameraPos, bounds)) continue;

                RENDERER.render(geometry, context);
            }

            RENDERER.renderEdges(activeEntries, context);
        } finally {
            matrices.pop();
        }
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