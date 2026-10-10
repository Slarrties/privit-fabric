package dev.slarrties.privit.client.render.adapter;

import dev.slarrties.privit.client.render.face.*;
import dev.slarrties.privit.client.render.edge.*;
import dev.slarrties.privit.client.render.config.VanillaRenderProfile;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;

import net.minecraft.client.render.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;

public final class VanillaAdapter implements RenderAdapter {
    private final VanillaRenderProfile profile;
    private final FaceTintCache faceTintCache;
    private final EdgeTintCache edgeTintCache;
    private final FaceLayers faceLayers;

    public VanillaAdapter(VanillaRenderProfile profile, FaceTintCache faceTintCache, EdgeTintCache edgeTintCache, FaceLayers faceLayers) {
        this.profile = Objects.requireNonNull(profile);
        this.faceTintCache = Objects.requireNonNull(faceTintCache);
        this.edgeTintCache = Objects.requireNonNull(edgeTintCache);
        this.faceLayers = Objects.requireNonNull(faceLayers);
    }

    @Override
    public void renderFaces(List<FaceMesh> meshes, WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        float inset = profile.inset(), scale = profile.textureScale();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay = OverlayTexture.DEFAULT_UV;

        for (FaceMesh mesh : meshes) {
            if (mesh.isEmpty()) continue;

            for (Face face : mesh.faces()) {
                VertexConsumer buffer = consumers.getBuffer(faceLayers.getLayer(face.type()));
                FaceTint tint = faceTintCache.get(profile, face.color(), face.type());
                face.emitQuad(
                        (x, y, z, u, v) -> buffer.vertex(matrix, x, y, z)
                        .color(tint.red(), tint.green(), tint.blue(), tint.alpha())
                        .texture(u, v).overlay(overlay)
                        .light(light)
                        .normal(0, 1, 0), inset, scale);
            }
        }
    }

    @Override
    public void renderEdges(List<RegionRenderEntry> entries, WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer edgeBuffer = consumers.getBuffer(EdgeLayers.LINE_LAYER);
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        for (RegionRenderEntry entry : entries) {
            EdgeTint tint = edgeTintCache.get(profile, entry.color());
            float r = tint.red(), g = tint.green(), b = tint.blue(), a = tint.alpha();

            for (Edge edge : entry.cachedEdges().solid()) emitSolidLine(edgeBuffer, matrix, edge, r, g, b, a);
            for (Edge edge : entry.cachedEdges().dashed()) emitDashedLine(edgeBuffer, matrix, edge, r, g, b, a);
        }
    }

    private void emitSolidLine(VertexConsumer buffer, Matrix4f matrix, Edge edge, float r, float g, float b, float a) {
        float dx = (float) (edge.x2() - edge.x1()), dy = (float) (edge.y2() - edge.y1()), dz = (float) (edge.z2() - edge.z1());
        float len = Math.max((float) Math.sqrt(dx * dx + dy * dy + dz * dz), 1e-6f);
        float nx = dx / len, ny = dy / len, nz = dz / len;

        buffer.vertex(matrix, (float) edge.x1(), (float) edge.y1(), (float) edge.z1()).color(r, g, b, a).normal(nx, ny, nz);
        buffer.vertex(matrix, (float) edge.x2(), (float) edge.y2(), (float) edge.z2()).color(r, g, b, a).normal(nx, ny, nz);
    }

    private void emitDashedLine(VertexConsumer buffer, Matrix4f matrix, Edge edge, float r, float g, float b, float a) {
        double dx = edge.x2() - edge.x1(), dy = edge.y2() - edge.y1(), dz = edge.z2() - edge.z1();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001) return;

        float nx = (float) (dx / length), ny = (float) (dy / length), nz = (float) (dz / length);
        double dash = profile.dashLength(), gap = profile.dashGap(), seg = dash + gap;
        int segments = (int) Math.ceil(length / seg);

        for (int i = 0; i < segments; i++) {
            double t1 = i * seg / length, t2 = Math.min((i * seg + dash) / length, 1.0);
            if (t1 >= 1.0) break;

            float sx = (float) (edge.x1() + dx * t1), sy = (float) (edge.y1() + dy * t1), sz = (float) (edge.z1() + dz * t1);
            float ex = (float) (edge.x1() + dx * t2), ey = (float) (edge.y1() + dy * t2), ez = (float) (edge.z1() + dz * t2);

            buffer.vertex(matrix, sx, sy, sz).color(r, g, b, a).normal(nx, ny, nz);
            buffer.vertex(matrix, ex, ey, ez).color(r, g, b, a).normal(nx, ny, nz);
        }
    }
}