package dev.slarrties.privit.client.render.adapter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.slarrties.privit.client.render.face.*;
import dev.slarrties.privit.client.render.edge.Edge;
import dev.slarrties.privit.client.render.edge.EdgeTint;
import dev.slarrties.privit.client.render.edge.EdgeTintCache;
import dev.slarrties.privit.client.render.config.RenderConfig;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;

public class RenderAdapter {
    private final RenderConfig profile;
    private final FaceTintCache faceTintCache;
    private final EdgeTintCache edgeTintCache;
    private final FaceLayers faceLayers;

    public RenderAdapter(RenderConfig profile,
                         FaceTintCache faceTintCache,
                         EdgeTintCache edgeTintCache,
                         FaceLayers faceLayers) {
        this.profile = Objects.requireNonNull(profile);
        this.faceTintCache = Objects.requireNonNull(faceTintCache);
        this.edgeTintCache = Objects.requireNonNull(edgeTintCache);
        this.faceLayers = Objects.requireNonNull(faceLayers);
    }

    public void renderFaces(List<FaceMesh> meshes, LevelRenderContext context) {
        MultiBufferSource.BufferSource bufferSource = context.bufferSource();
        if (bufferSource == null) return;

        PoseStack poseStack = context.poseStack();
        Matrix4f matrix = poseStack.last().pose();
        float inset = profile.inset(), scale = profile.textureScale();

        for (FaceMesh mesh : meshes) {
            if (mesh.isEmpty()) continue;
            for (Face face : mesh.faces()) {
                RenderType currentLayer = faceLayers.getLayer(face.type());
                VertexConsumer buffer = bufferSource.getBuffer(currentLayer);
                FaceTint tint = faceTintCache.get(profile, face.color(), face.type());

                face.emitQuad(
                        (x, y, z, u, v, nx, ny, nz) -> buffer.addVertex(matrix, x, y, z)
                                .setColor(tint.red(), tint.green(), tint.blue(), tint.alpha())
                                .setUv(u, v)
                                .setUv1(0, 10)
                                .setUv2(0xF0, 0xF0)
                                .setNormal(nx, ny, nz),
                        inset, scale);
            }
        }
    }

    public void renderEdges(List<RegionRenderEntry> entries, LevelRenderContext context, RenderType edgeLayer) {
        MultiBufferSource.BufferSource bufferSource = context.bufferSource();
        if (bufferSource == null) return;

        VertexConsumer edgeBuffer = bufferSource.getBuffer(edgeLayer);
        PoseStack poseStack = context.poseStack();
        Matrix4f matrix = poseStack.last().pose();

        for (RegionRenderEntry entry : entries) {
            EdgeTint tint = edgeTintCache.get(profile, entry.color());
            float r = tint.red(), g = tint.green(), b = tint.blue(), a = tint.alpha();

            for (Edge edge : entry.cachedEdges().solid())
                emitSolidLine(edgeBuffer, matrix, edge, r, g, b, a);
            for (Edge edge : entry.cachedEdges().dashed())
                emitDashedLine(edgeBuffer, matrix, edge, r, g, b, a);
        }
    }

    private void emitSolidLine(VertexConsumer buffer, Matrix4f matrix, Edge edge,
                               float r, float g, float b, float a) {
        emitRibbon(buffer, matrix,
                (float) edge.x1(), (float) edge.y1(), (float) edge.z1(),
                (float) edge.x2(), (float) edge.y2(), (float) edge.z2(),
                r, g, b, a);
    }

    private void emitDashedLine(VertexConsumer buffer, Matrix4f matrix, Edge edge,
                                float r, float g, float b, float a) {
        double dx = edge.x2() - edge.x1(), dy = edge.y2() - edge.y1(), dz = edge.z2() - edge.z1();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001) return;

        double dash = profile.dashLength(), gap = profile.dashGap(), seg = dash + gap;
        int segments = (int) Math.ceil(length / seg);

        for (int i = 0; i < segments; i++) {
            double t1 = i * seg / length, t2 = Math.min((i * seg + dash) / length, 1.0);
            if (t1 >= 1.0) break;
            emitRibbon(buffer, matrix,
                    (float) (edge.x1() + dx * t1), (float) (edge.y1() + dy * t1), (float) (edge.z1() + dz * t1),
                    (float) (edge.x1() + dx * t2), (float) (edge.y1() + dy * t2), (float) (edge.z1() + dz * t2),
                    r, g, b, a);
        }
    }

    private void emitRibbon(VertexConsumer buffer, Matrix4f matrix,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = Math.max((float) Math.sqrt(dx * dx + dy * dy + dz * dz), 1e-6f);
        float nx = dx / len, ny = dy / len, nz = dz / len;

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        float midWorldX = (x1 + x2) * 0.5f;
        float midWorldY = (y1 + y2) * 0.5f;
        float midWorldZ = (z1 + z2) * 0.5f;

        float cx = (float) cameraPos.x - midWorldX;
        float cy = (float) cameraPos.y - midWorldY;
        float cz = (float) cameraPos.z - midWorldZ;

        float distance = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float hw = profile.edgeQuadHalfThickness() * (1.0f + distance * 0.08f);

        float sx = cy * nz - cz * ny;
        float sy = cz * nx - cx * nz;
        float sz = cx * ny - cy * nx;
        float slen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (slen < 1e-4f) {
            if (Math.abs(ny) < 0.9f) { sx = nz; sy = 0; sz = -nx; }
            else                     { sx = 0;  sy = -nz; sz = ny; }
            slen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        }
        sx = sx / slen * hw;
        sy = sy / slen * hw;
        sz = sz / slen * hw;

        float m0x = x1 - sx, m0y = y1 - sy, m0z = z1 - sz;
        float m1x = x2 - sx, m1y = y2 - sy, m1z = z2 - sz;
        float m2x = x2 + sx, m2y = y2 + sy, m2z = z2 + sz;
        float m3x = x1 + sx, m3y = y1 + sy, m3z = z1 + sz;

        float e1x = m1x - m0x, e1y = m1y - m0y, e1z = m1z - m0z;
        float e2x = m3x - m0x, e2y = m3y - m0y, e2z = m3z - m0z;
        float qnx = e1y * e2z - e1z * e2y;
        float qny = e1z * e2x - e1x * e2z;
        float qnz = e1x * e2y - e1y * e2x;
        if (qnx * cx + qny * cy + qnz * cz < 0) {
            float tx = m1x, ty = m1y, tz = m1z;
            m1x = m3x; m1y = m3y; m1z = m3z;
            m3x = tx;  m3y = ty;  m3z = tz;
        }

        buffer.addVertex(matrix, m0x, m0y, m0z).setColor(r, g, b, a).setUv(0.5f, 0.5f).setUv2(0xFF, 0xFF).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, m1x, m1y, m1z).setColor(r, g, b, a).setUv(0.5f, 0.5f).setUv2(0xFF, 0xFF).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, m2x, m2y, m2z).setColor(r, g, b, a).setUv(0.5f, 0.5f).setUv2(0xFF, 0xFF).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, m3x, m3y, m3z).setColor(r, g, b, a).setUv(0.5f, 0.5f).setUv2(0xFF, 0xFF).setNormal(nx, ny, nz);
    }
}