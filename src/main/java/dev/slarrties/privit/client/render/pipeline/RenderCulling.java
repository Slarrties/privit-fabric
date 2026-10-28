package dev.slarrties.privit.client.render.pipeline;

import dev.slarrties.privit.client.render.config.RenderConfig;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.culling.Frustum;

public final class RenderCulling {

    private RenderCulling() {}

    public static boolean isFrustumVisible(Frustum frustum, AABB box) {
        return frustum == null || frustum.isVisible(box);
    }

    public static boolean isWithinRenderDistance(Vec3 cameraPos, AABB box) {
        return cameraPos.distanceToSqr(box.getCenter()) <= RenderConfig.MAX_RENDER_DISTANCE_SQ;
    }
}