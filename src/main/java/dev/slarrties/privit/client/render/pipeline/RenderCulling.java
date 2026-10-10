package dev.slarrties.privit.client.render.pipeline;

import dev.slarrties.privit.client.render.config.RegionRenderConfig;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.Frustum;

public final class RenderCulling {
    private RenderCulling() {}
    public static boolean isFrustumVisible(Frustum frustum, Box box) { return frustum == null || frustum.isVisible(box); }
    public static boolean isWithinRenderDistance(Vec3d cameraPos, Box box) {
        return cameraPos.squaredDistanceTo(box.getCenter()) <= RegionRenderConfig.MAX_RENDER_DISTANCE_SQ;
    }
}