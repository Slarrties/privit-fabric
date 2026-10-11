package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.client.render.state.RenderType;
import dev.slarrties.privit.common.region.Color;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

import java.util.List;

public record Face(
        RenderType type, Direction faceDirection,
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        double minU, double minV, double maxU, double maxV,
        Color color, float baseAlpha
) {
    public interface QuadEmitter { void emit(float x, float y, float z, float u, float v); }

    public boolean isEmpty() { return minU >= maxU || minV >= maxV; }

    public boolean intersects(Face other) {
        if (this.faceDirection != other.faceDirection) return false;
        return !(maxU <= other.minU || minU >= other.maxU || maxV <= other.minV || minV >= other.maxV);
    }

    public static List<Face> fromBox(BlockBox box, RenderType type, Color color, float alpha) {
        if (box == null) return List.of();

        double minX = box.getMinX(), minY = box.getMinY(), minZ = box.getMinZ();
        double maxX = box.getMaxX() + 1.0, maxY = box.getMaxY() + 1.0, maxZ = box.getMaxZ() + 1.0;

        return List.of(
                fromPlane(type, Direction.DOWN, minY, minX, minZ, maxX, maxZ, color, alpha),
                fromPlane(type, Direction.UP, maxY, minX, minZ, maxX, maxZ, color, alpha),
                fromPlane(type, Direction.NORTH, minZ, minX, minY, maxX, maxY, color, alpha),
                fromPlane(type, Direction.SOUTH, maxZ, minX, minY, maxX, maxY, color, alpha),
                fromPlane(type, Direction.WEST, minX, minZ, minY, maxZ, maxY, color, alpha),
                fromPlane(type, Direction.EAST, maxX, minZ, minY, maxZ, maxY, color, alpha)
        );
    }

    public static Face fromPlane(RenderType type, Direction dir, double fixedCoord,
                                 double minU, double minV, double maxU, double maxV, Color color, float alpha) {
        return switch (dir) {
            case DOWN  -> new Face(type, dir, minU, fixedCoord, minV, maxU, fixedCoord, maxV, minU, minV, maxU, maxV, color, alpha);
            case UP    -> new Face(type, dir, minU, fixedCoord, minV, maxU, fixedCoord, maxV, minU, minV, maxU, maxV, color, alpha);
            case NORTH -> new Face(type, dir, minU, minV, fixedCoord, maxU, maxV, fixedCoord, minU, minV, maxU, maxV, color, alpha);
            case SOUTH -> new Face(type, dir, minU, minV, fixedCoord, maxU, maxV, fixedCoord, minU, minV, maxU, maxV, color, alpha);
            case WEST  -> new Face(type, dir, fixedCoord, minV, minU, fixedCoord, maxV, maxU, minU, minV, maxU, maxV, color, alpha);
            case EAST  -> new Face(type, dir, fixedCoord, minV, minU, fixedCoord, maxV, maxU, minU, minV, maxU, maxV, color, alpha);
        };
    }

    public void emitQuad(QuadEmitter emitter, float inset, float scale) {
        float minXf = (float) minX, minYf = (float) minY, minZf = (float) minZ;
        float maxXf = (float) maxX, maxYf = (float) maxY, maxZf = (float) maxZ;

        switch (faceDirection) {
            case DOWN -> {
                float y = minYf + inset;
                emitter.emit(minXf, y, minZf, minXf*scale, minZf*scale);
                emitter.emit(maxXf, y, minZf, maxXf*scale, minZf*scale);
                emitter.emit(maxXf, y, maxZf, maxXf*scale, maxZf*scale);
                emitter.emit(minXf, y, maxZf, minXf*scale, maxZf*scale);
            }
            case UP -> {
                float y = maxYf - inset;
                emitter.emit(minXf, y, minZf, minXf*scale, minZf*scale);
                emitter.emit(minXf, y, maxZf, minXf*scale, maxZf*scale);
                emitter.emit(maxXf, y, maxZf, maxXf*scale, maxZf*scale);
                emitter.emit(maxXf, y, minZf, maxXf*scale, minZf*scale);
            }
            case NORTH -> {
                float z = minZf + inset;
                emitter.emit(minXf, minYf, z, minXf*scale, minYf*scale);
                emitter.emit(maxXf, minYf, z, maxXf*scale, minYf*scale);
                emitter.emit(maxXf, maxYf, z, maxXf*scale, maxYf*scale);
                emitter.emit(minXf, maxYf, z, minXf*scale, maxYf*scale);
            }
            case SOUTH -> {
                float z = maxZf - inset;
                emitter.emit(minXf, minYf, z, minXf*scale, minYf*scale);
                emitter.emit(minXf, maxYf, z, minXf*scale, maxYf*scale);
                emitter.emit(maxXf, maxYf, z, maxXf*scale, maxYf*scale);
                emitter.emit(maxXf, minYf, z, maxXf*scale, minYf*scale);
            }
            case WEST -> {
                float x = minXf + inset;
                emitter.emit(x, minYf, minZf, minZf*scale, minYf*scale);
                emitter.emit(x, minYf, maxZf, maxZf*scale, minYf*scale);
                emitter.emit(x, maxYf, maxZf, maxZf*scale, maxYf*scale);
                emitter.emit(x, maxYf, minZf, minZf*scale, maxYf*scale);
            }
            case EAST -> {
                float x = maxXf - inset;
                emitter.emit(x, minYf, minZf, minZf*scale, minYf*scale);
                emitter.emit(x, maxYf, minZf, minZf*scale, maxYf*scale);
                emitter.emit(x, maxYf, maxZf, maxZf*scale, maxYf*scale);
                emitter.emit(x, minYf, maxZf, maxZf*scale, minYf*scale);
            }
        }
    }
}