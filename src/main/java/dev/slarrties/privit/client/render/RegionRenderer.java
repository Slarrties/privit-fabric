package dev.slarrties.privit.client.render;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import org.joml.Matrix4f;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

public class RegionRenderer {

    private static final float TEXTURE_SCALE = 1.0f;
    private static final float EDGE_EXPAND = 0.001f;

    public void render(RegionGeometry geometry, WorldRenderContext context) {
        if (geometry == null || geometry.isEmpty()) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        MatrixStack matrices = context.matrixStack();
        matrices.push();

        try {
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            for (RegionFace face : geometry.getFaces()) {
                VertexConsumer buffer = getBuffer(consumers, face.type());
                drawFace(buffer, matrix, face);
            }
        } finally {
            matrices.pop();
        }
    }

    private VertexConsumer getBuffer(VertexConsumerProvider consumers, RenderType type) {
        return switch (type) {
            case ORIGINAL -> consumers.getBuffer(RegionRenderLayers.ORIGINAL);
            case CONFLICT -> consumers.getBuffer(RegionRenderLayers.CONFLICT);
            case DRAFT    -> consumers.getBuffer(RegionRenderLayers.getDraftLayer());
        };
    }

    private void drawFace(VertexConsumer buffer, Matrix4f matrix, RegionFace face) {
        float r = face.getRenderProperties().red();
        float g = face.getRenderProperties().green();
        float b = face.getRenderProperties().blue();
        float a = face.getRenderProperties().alpha();

        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int overlay = OverlayTexture.DEFAULT_UV;

        double[] coords = applyInset(face);
        float minX = (float) coords[0], minY = (float) coords[1], minZ = (float) coords[2];
        float maxX = (float) coords[3], maxY = (float) coords[4], maxZ = (float) coords[5];

        float[] uvs = calculateUV(face.faceDirection(), minX, minY, minZ, maxX, maxY, maxZ);

        emitQuad(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ,
                face.faceDirection(), r, g, b, a, light, overlay, uvs);
    }

    private double[] applyInset(RegionFace face) {
        double inset = RegionRenderConfig.INSET;
        double minX = face.minX(), minY = face.minY(), minZ = face.minZ();
        double maxX = face.maxX(), maxY = face.maxY(), maxZ = face.maxZ();

        return switch (face.faceDirection()) {
            case DOWN  -> new double[]{minX, minY + inset, minZ, maxX, minY + inset, maxZ};
            case UP    -> new double[]{minX, maxY - inset, minZ, maxX, maxY - inset, maxZ};
            case NORTH -> new double[]{minX, minY, minZ + inset, maxX, maxY, minZ + inset};
            case SOUTH -> new double[]{minX, minY, maxZ - inset, maxX, maxY, maxZ - inset};
            case WEST  -> new double[]{minX + inset, minY, minZ, minX + inset, maxY, maxZ};
            case EAST  -> new double[]{maxX - inset, minY, minZ, maxX - inset, maxY, maxZ};
        };
    }

    private float[] calculateUV(Direction dir, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ) {
        return switch (dir) {
            case DOWN ->  new float[]{minX * TEXTURE_SCALE, minZ * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, minZ * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, maxZ * TEXTURE_SCALE,
                    minX * TEXTURE_SCALE, maxZ * TEXTURE_SCALE};

            case UP ->    new float[]{minX * TEXTURE_SCALE, minZ * TEXTURE_SCALE,
                    minX * TEXTURE_SCALE, maxZ * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, maxZ * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, minZ * TEXTURE_SCALE};

            case NORTH -> new float[]{minX * TEXTURE_SCALE, minY * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, minY * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, maxY * TEXTURE_SCALE,
                    minX * TEXTURE_SCALE, maxY * TEXTURE_SCALE};

            case SOUTH -> new float[]{minX * TEXTURE_SCALE, minY * TEXTURE_SCALE,
                    minX * TEXTURE_SCALE, maxY * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, maxY * TEXTURE_SCALE,
                    maxX * TEXTURE_SCALE, minY * TEXTURE_SCALE};

            case WEST ->  new float[]{minZ * TEXTURE_SCALE, minY * TEXTURE_SCALE,
                    maxZ * TEXTURE_SCALE, minY * TEXTURE_SCALE,
                    maxZ * TEXTURE_SCALE, maxY * TEXTURE_SCALE,
                    minZ * TEXTURE_SCALE, maxY * TEXTURE_SCALE};

            case EAST ->  new float[]{minZ * TEXTURE_SCALE, minY * TEXTURE_SCALE,
                    minZ * TEXTURE_SCALE, maxY * TEXTURE_SCALE,
                    maxZ * TEXTURE_SCALE, maxY * TEXTURE_SCALE,
                    maxZ * TEXTURE_SCALE, minY * TEXTURE_SCALE};
        };
    }

    private void emitQuad(VertexConsumer buffer, Matrix4f mat,
                          float minX, float minY, float minZ,
                          float maxX, float maxY, float maxZ,
                          Direction dir, float r, float g, float b, float a,
                          int light, int overlay, float[] uvs) {

        float u1 = uvs[0], v1 = uvs[1], u2 = uvs[2], v2 = uvs[3],
                u3 = uvs[4], v3 = uvs[5], u4 = uvs[6], v4 = uvs[7];

        switch (dir) {
            case DOWN  -> quad(buffer, mat, minX,minY,minZ, maxX,minY,minZ, maxX,minY,maxZ, minX,minY,maxZ, r,g,b,a, light, overlay, u1,v1,u2,v2,u3,v3,u4,v4);
            case UP    -> quad(buffer, mat, minX,maxY,minZ, minX,maxY,maxZ, maxX,maxY,maxZ, maxX,maxY,minZ, r,g,b,a, light, overlay, u1,v1,u2,v2,u3,v3,u4,v4);
            case NORTH -> quad(buffer, mat, minX,minY,minZ, maxX,minY,minZ, maxX,maxY,minZ, minX,maxY,minZ, r,g,b,a, light, overlay, u1,v1,u2,v2,u3,v3,u4,v4);
            case SOUTH -> quad(buffer, mat, minX,minY,maxZ, minX,maxY,maxZ, maxX,maxY,maxZ, maxX,minY,maxZ, r,g,b,a, light, overlay, u1,v1,u2,v2,u3,v3,u4,v4);
            case WEST  -> quad(buffer, mat, minX,minY,minZ, minX,minY,maxZ, minX,maxY,maxZ, minX,maxY,minZ, r,g,b,a, light, overlay, u1,v1,u2,v2,u3,v3,u4,v4);
            case EAST  -> quad(buffer, mat, maxX,minY,minZ, maxX,maxY,minZ, maxX,maxY,maxZ, maxX,minY,maxZ, r,g,b,a, light, overlay, u1,v1,u2,v2,u3,v3,u4,v4);
        }
    }

    private void quad(VertexConsumer buffer, Matrix4f mat,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4,
                      float r, float g, float b, float a,
                      int light, int overlay,
                      float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4) {

        buffer.vertex(mat, x1, y1, z1).color(r, g, b, a).texture(u1, v1).overlay(overlay).light(light).normal(0, 1, 0);
        buffer.vertex(mat, x2, y2, z2).color(r, g, b, a).texture(u2, v2).overlay(overlay).light(light).normal(0, 1, 0);
        buffer.vertex(mat, x3, y3, z3).color(r, g, b, a).texture(u3, v3).overlay(overlay).light(light).normal(0, 1, 0);
        buffer.vertex(mat, x4, y4, z4).color(r, g, b, a).texture(u4, v4).overlay(overlay).light(light).normal(0, 1, 0);
    }

    private void drawEdges(RegionGeometry geometry, WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer edgeBuffer = consumers.getBuffer(RegionRenderLayers.EDGE_LAYER);
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        RenderType type = geometry.getType();
        boolean dashed = RegionRenderConfig.isDashed(type);
        float thickness = RegionRenderConfig.getEdgeThickness(type);

        Set<NormalizedEdge> uniqueEdges = new HashSet<>();
        for (RegionFace face : geometry.getFaces()) {
            for (Edge edge : getFaceEdges(face)) {
                uniqueEdges.add(new NormalizedEdge(edge));
            }
        }

        RenderProperties props = geometry.getFaces().get(0).getRenderProperties();
        float r = props.edgeRed();
        float g = props.edgeGreen();
        float b = props.edgeBlue();
        float a = props.edgeAlpha();

        for (NormalizedEdge edge : uniqueEdges) {
            if (dashed) {
                emitDashedLine(edgeBuffer, matrix, edge, r, g, b, a);
            } else {
                emitSolidLine(edgeBuffer, matrix, edge, r, g, b, a);
            }
        }
    }

    private Edge[] getFaceEdges(RegionFace face) {
        double minX = face.minX() - EDGE_EXPAND;
        double minY = face.minY() - EDGE_EXPAND;
        double minZ = face.minZ() - EDGE_EXPAND;
        double maxX = face.maxX() + EDGE_EXPAND;
        double maxY = face.maxY() + EDGE_EXPAND;
        double maxZ = face.maxZ() + EDGE_EXPAND;

        return switch (face.faceDirection()) {
            case DOWN, UP -> new Edge[]{
                    new Edge(minX, minY, minZ, maxX, minY, minZ),
                    new Edge(maxX, minY, minZ, maxX, minY, maxZ),
                    new Edge(maxX, minY, maxZ, minX, minY, maxZ),
                    new Edge(minX, minY, maxZ, minX, minY, minZ)
            };
            case NORTH, SOUTH -> new Edge[]{
                    new Edge(minX, minY, minZ, maxX, minY, minZ),
                    new Edge(maxX, minY, minZ, maxX, maxY, minZ),
                    new Edge(maxX, maxY, minZ, minX, maxY, minZ),
                    new Edge(minX, maxY, minZ, minX, minY, minZ)
            };
            case WEST, EAST -> new Edge[]{
                    new Edge(minX, minY, minZ, minX, minY, maxZ),
                    new Edge(minX, minY, maxZ, minX, maxY, maxZ),
                    new Edge(minX, maxY, maxZ, minX, maxY, minZ),
                    new Edge(minX, maxY, minZ, minX, minY, minZ)
            };
        };
    }

    private record Edge(double x1, double y1, double z1, double x2, double y2, double z2) {}

    private record NormalizedEdge(double x1, double y1, double z1, double x2, double y2, double z2) {
        public NormalizedEdge(Edge e) {
            this(Math.min(e.x1, e.x2), Math.min(e.y1, e.y2), Math.min(e.z1, e.z2),
                    Math.max(e.x1, e.x2), Math.max(e.y1, e.y2), Math.max(e.z1, e.z2));
        }
    }

    public void renderEdges(List<RegionRenderEntry> entries, WorldRenderContext context) {
        if (entries.isEmpty()) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer edgeBuffer = consumers.getBuffer(RegionRenderLayers.EDGE_LAYER);
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        Set<NormalizedEdge> drawnEdges = new HashSet<>();

        for (RegionRenderEntry entry : entries) {
            RenderProperties props = RenderProperties.fromColor(entry.color(), 1.0f, false);
            float r = props.edgeRed();
            float g = props.edgeGreen();
            float b = props.edgeBlue();
            float a = props.edgeAlpha();

            for (BlockBox conflictBox : entry.conflicts()) {
                drawBoxEdges(conflictBox, edgeBuffer, matrix, true, r, g, b, a, drawnEdges);
            }

            if (entry.hasOriginal()) {
                drawBoxEdgesNonIntersecting(entry.original(), edgeBuffer, matrix, false, r, g, b, a, entry.conflicts(), drawnEdges);
            }

            if (entry.hasDraft()) {
                drawBoxEdgesNonIntersecting(entry.draft(), edgeBuffer, matrix, true, r, g, b, a, entry.conflicts(), drawnEdges);
            }
        }
    }

    private void drawBoxEdgesNonIntersecting(BlockBox box, VertexConsumer buffer, Matrix4f matrix,
                                             boolean dashed, float r, float g, float b, float a,
                                             List<BlockBox> conflicts, Set<NormalizedEdge> drawn) {
        if (box == null) return;

        Edge[] edges = getBoxEdges(box);
        for (Edge edge : edges) {
            if (intersectsAnyConflict(edge, conflicts)) continue;

            NormalizedEdge ne = new NormalizedEdge(edge);
            if (drawn.add(ne)) {
                if (dashed) {
                    emitDashedLine(buffer, matrix, ne, r, g, b, a);
                } else {
                    emitSolidLine(buffer, matrix, ne, r, g, b, a);
                }
            }
        }
    }

    private void drawBoxEdgesIfNotCovered(BlockBox box, VertexConsumer buffer, Matrix4f matrix,
                                          boolean dashed, float r, float g, float b, float a,
                                          List<BlockBox> conflicts, Set<NormalizedEdge> drawn) {
        if (box == null) return;
        Edge[] edges = getBoxEdges(box);
        for (Edge e : edges) {
            if (isEdgeFullyCovered(e, conflicts)) continue;

            NormalizedEdge ne = new NormalizedEdge(e);
            if (drawn.add(ne)) {
                if (dashed) emitDashedLine(buffer, matrix, ne, r, g, b, a);
                else emitSolidLine(buffer, matrix, ne, r, g, b, a);
            }
        }
    }

    private boolean intersectsAnyConflict(Edge edge, List<BlockBox> conflicts) {
        for (BlockBox c : conflicts) {
            if (boxesIntersectAlongEdge(edge, c)) {
                return true;
            }
        }
        return false;
    }

    private boolean boxesIntersectAlongEdge(Edge edge, BlockBox conflict) {
        double minX = Math.min(edge.x1, edge.x2);
        double maxX = Math.max(edge.x1, edge.x2);
        double minY = Math.min(edge.y1, edge.y2);
        double maxY = Math.max(edge.y1, edge.y2);
        double minZ = Math.min(edge.z1, edge.z2);
        double maxZ = Math.max(edge.z1, edge.z2);

        return !(maxX < conflict.getMinX() || minX > conflict.getMaxX() + 1 ||
                maxY < conflict.getMinY() || minY > conflict.getMaxY() + 1 ||
                maxZ < conflict.getMinZ() || minZ > conflict.getMaxZ() + 1);
    }

    private void drawBoxEdges(BlockBox box, VertexConsumer buffer, Matrix4f matrix,
                              boolean dashed, float r, float g, float b, float a,
                              Set<NormalizedEdge> drawn) {
        Edge[] edges = getBoxEdges(box);
        for (Edge e : edges) {
            NormalizedEdge ne = new NormalizedEdge(e);
            if (drawn.add(ne)) {
                if (dashed) emitDashedLine(buffer, matrix, ne, r, g, b, a);
                else emitSolidLine(buffer, matrix, ne, r, g, b, a);
            }
        }
    }

    private Edge[] getBoxEdges(BlockBox box) {
        double minX = box.getMinX() - EDGE_EXPAND;
        double minY = box.getMinY() - EDGE_EXPAND;
        double minZ = box.getMinZ() - EDGE_EXPAND;
        double maxX = box.getMaxX() + 1.0 + EDGE_EXPAND;
        double maxY = box.getMaxY() + 1.0 + EDGE_EXPAND;
        double maxZ = box.getMaxZ() + 1.0 + EDGE_EXPAND;

        return new Edge[]{
                // Bottom
                new Edge(minX, minY, minZ, maxX, minY, minZ),   // south
                new Edge(maxX, minY, minZ, maxX, minY, maxZ),   // east
                new Edge(maxX, minY, maxZ, minX, minY, maxZ),   // north
                new Edge(minX, minY, maxZ, minX, minY, minZ),   // west

                // Top
                new Edge(minX, maxY, minZ, maxX, maxY, minZ),   // south
                new Edge(maxX, maxY, minZ, maxX, maxY, maxZ),   // east
                new Edge(maxX, maxY, maxZ, minX, maxY, maxZ),   // north
                new Edge(minX, maxY, maxZ, minX, maxY, minZ),   // west

                // Vertical
                new Edge(minX, minY, minZ, minX, maxY, minZ),   // southwest
                new Edge(maxX, minY, minZ, maxX, maxY, minZ),   // southeast
                new Edge(maxX, minY, maxZ, maxX, maxY, maxZ),   // northeast
                new Edge(minX, minY, maxZ, minX, maxY, maxZ)    // northwest
        };
    }

    private boolean isEdgeFullyCovered(Edge edge, List<BlockBox> conflicts) {
        for (BlockBox box : conflicts) {
            if (isPointInBox(edge.x1, edge.y1, edge.z1, box) &&
                    isPointInBox(edge.x2, edge.y2, edge.z2, box)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointInBox(double x, double y, double z, BlockBox box) {
        return x >= box.getMinX() && x <= box.getMaxX() + 1 &&
                y >= box.getMinY() && y <= box.getMaxY() + 1 &&
                z >= box.getMinZ() && z <= box.getMaxZ() + 1;
    }

    private void emitSolidLine(VertexConsumer buffer, Matrix4f matrix, NormalizedEdge edge, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) edge.x1, (float) edge.y1, (float) edge.z1).color(r, g, b, a).normal(0, 0, 0);
        buffer.vertex(matrix, (float) edge.x2, (float) edge.y2, (float) edge.z2).color(r, g, b, a).normal(0, 0, 0);
    }

    private void emitDashedLine(VertexConsumer buffer, Matrix4f matrix, NormalizedEdge edge, float r, float g, float b, float a) {
        double dx = edge.x2 - edge.x1;
        double dy = edge.y2 - edge.y1;
        double dz = edge.z2 - edge.z1;
        double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (length < 0.001) return;

        double dash = RegionRenderConfig.EDGE_DASH_LENGTH_BLOCKS;
        double gap = RegionRenderConfig.EDGE_DASH_GAP_BLOCKS;
        double seg = dash + gap;
        int segments = (int) Math.ceil(length / seg);

        for (int i = 0; i < segments; i++) {
            double t1 = i * seg / length;
            double t2 = Math.min((i * seg + dash) / length, 1.0);
            if (t1 >= 1.0) break;

            double sx = edge.x1 + dx * t1;
            double sy = edge.y1 + dy * t1;
            double sz = edge.z1 + dz * t1;
            double ex = edge.x1 + dx * t2;
            double ey = edge.y1 + dy * t2;
            double ez = edge.z1 + dz * t2;

            buffer.vertex(matrix, (float) sx, (float) sy, (float) sz).color(r, g, b, a).normal(0, 0, 0);
            buffer.vertex(matrix, (float) ex, (float) ey, (float) ez).color(r, g, b, a).normal(0, 0, 0);
        }
    }
}