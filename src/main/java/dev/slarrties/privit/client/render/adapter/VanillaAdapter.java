package dev.slarrties.privit.client.render.adapter;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.slarrties.privit.client.render.face.*;
import dev.slarrties.privit.client.render.edge.*;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;
import dev.slarrties.privit.client.render.config.VanillaRenderProfile;

import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

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
                                .texture(u, v)
                                .overlay(overlay)
                                .light(light)
                                .normal(0, 1, 0)
                                .next(),
                        inset, scale);
            }
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw();
    }

    @Override
    public void renderEdges(List<RegionRenderEntry> entries, WorldRenderContext context) {
        if (entries.isEmpty()) return;

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Vec3d cameraPos = context.camera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder edges = Tessellator.getInstance().getBuffer();
        edges.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float halfWidth = profile.edgeQuadHalfThickness();

        for (RegionRenderEntry entry : entries) {
            EdgeTint tint = edgeTintCache.get(profile, entry.color());
            float r = tint.red(), g = tint.green(), b = tint.blue(), a = tint.alpha();

            for (Edge edge : entry.cachedEdges().solid())
                emitEdgeQuad(edges, matrix, edge.x1(), edge.y1(), edge.z1(), edge.x2(), edge.y2(), edge.z2(), cameraPos, halfWidth, r, g, b, a);

            for (Edge edge : entry.cachedEdges().dashed())
                emitDashedQuad(edges, matrix, cameraPos, edge.x1(), edge.y1(), edge.z1(), edge.x2(), edge.y2(), edge.z2(), halfWidth, r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(edges.end());
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private void emitEdgeQuad(VertexConsumer buffer, Matrix4f matrix,
                              double x1, double y1, double z1,
                              double x2, double y2, double z2,
                              Vec3d cameraWorld, float halfWidth,
                              float r, float g, float b, float a) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4) return;

        double mx = (x1 + x2) * 0.5 - cameraWorld.x;
        double my = (y1 + y2) * 0.5 - cameraWorld.y;
        double mz = (z1 + z2) * 0.5 - cameraWorld.z;

        double cx = dy * mz - dz * my;
        double cy = dz * mx - dx * mz;
        double cz = dx * my - dy * mx;
        double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (cl < 1e-4) { cx = 0; cy = 1; cz = 0; cl = 1; }

        double ox = cx / cl * halfWidth;
        double oy = cy / cl * halfWidth;
        double oz = cz / cl * halfWidth;

        buffer.vertex(matrix, (float) (x1 - ox), (float) (y1 - oy), (float) (z1 - oz)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float) (x2 - ox), (float) (y2 - oy), (float) (z2 - oz)).color(r, g, b, a).next();
    }

    private void emitDashedQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d cameraPos,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float halfWidth, float r, float g, float b, float a) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001) return;

        double dash = profile.dashLength(), gap = profile.dashGap(), seg = dash + gap;
        int segments = (int) Math.ceil(length / seg);

        for (int i = 0; i < segments; i++) {
            double t1 = i * seg / length;
            double t2 = Math.min((i * seg + dash) / length, 1.0);
            if (t1 >= 1.0) break;

            emitEdgeQuad(buffer, matrix,
                    x1 + dx * t1, y1 + dy * t1, z1 + dz * t1,
                    x1 + dx * t2, y1 + dy * t2, z1 + dz * t2,
                    cameraPos, halfWidth, r, g, b, a);
        }
    }
}