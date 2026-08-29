package dev.slarrties.privit.client.render;

import dev.slarrties.privit.common.region.Color;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

import java.util.List;

public record RegionFace(
        RenderType type,
        Direction faceDirection,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        double minU, double minV,
        double maxU, double maxV,
        double inset,
        boolean drawEdges,
        RenderProperties renderProperties
) {

    public static List<RegionFace> fromBox(BlockBox box, RenderType type, Color color, float alpha) {
        if (box == null) return List.of();

        double minX = box.getMinX();
        double minY = box.getMinY();
        double minZ = box.getMinZ();
        double maxX = box.getMaxX() + 1.0;
        double maxY = box.getMaxY() + 1.0;
        double maxZ = box.getMaxZ() + 1.0;
        boolean isDashed = (type == RenderType.DRAFT || type == RenderType.CONFLICT);
        RenderProperties props = RenderProperties.fromColor(color, alpha, isDashed);

        return List.of(
                createFace(type, Direction.DOWN,  minX, minY, minZ, maxX, minY, maxZ, minX, minZ, maxX, maxZ, props),
                createFace(type, Direction.UP,    minX, maxY, minZ, maxX, maxY, maxZ, minX, minZ, maxX, maxZ, props),
                createFace(type, Direction.NORTH, minX, minY, minZ, maxX, maxY, minZ, minX, minY, maxX, maxY, props),
                createFace(type, Direction.SOUTH, minX, minY, maxZ, maxX, maxY, maxZ, minX, minY, maxX, maxY, props),
                createFace(type, Direction.WEST,  minX, minY, minZ, minX, maxY, maxZ, minZ, minY, maxZ, maxY, props),
                createFace(type, Direction.EAST,  maxX, minY, minZ, maxX, maxY, maxZ, minZ, minY, maxZ, maxY, props)
        );
    }

    private static RegionFace createFace(RenderType type, Direction dir,
                                         double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ,
                                         double minU, double minV, double maxU, double maxV,
                                         RenderProperties props) {
        return new RegionFace(type, dir,
                minX, minY, minZ, maxX, maxY, maxZ,
                minU, minV, maxU, maxV,
                RegionRenderConfig.INSET,
                true,
                props);
    }

    public static RegionFace from2D(RenderType type, Direction dir, double fixedCoord,
                                    double minU, double minV, double maxU, double maxV,
                                    RenderProperties renderProperties) {
        double inset = RegionRenderConfig.INSET;

        return switch (dir) {
            case DOWN -> new RegionFace(type, dir,
                    minU, fixedCoord + inset, minV, maxU, fixedCoord + inset, maxV,
                    minU, minV, maxU, maxV, inset, true, renderProperties);

            case UP -> new RegionFace(type, dir,
                    minU, fixedCoord - inset, minV, maxU, fixedCoord - inset, maxV,
                    minU, minV, maxU, maxV, inset, true, renderProperties);

            case NORTH -> new RegionFace(type, dir,
                    minU, minV, fixedCoord + inset, maxU, maxV, fixedCoord + inset,
                    minU, minV, maxU, maxV, inset, true, renderProperties);

            case SOUTH -> new RegionFace(type, dir,
                    minU, minV, fixedCoord - inset, maxU, maxV, fixedCoord - inset,
                    minU, minV, maxU, maxV, inset, true, renderProperties);

            case WEST -> new RegionFace(type, dir,
                    fixedCoord + inset, minV, minU, fixedCoord + inset, maxV, maxU,
                    minU, minV, maxU, maxV, inset, true, renderProperties);

            case EAST -> new RegionFace(type, dir,
                    fixedCoord - inset, minV, minU, fixedCoord - inset, maxV, maxU,
                    minU, minV, maxU, maxV, inset, true, renderProperties);
        };
    }

    public static RegionFace fromBoxEntry(RenderType type, Color color, Direction dir, double fixedCoord,
                                          double minU, double minV, double maxU, double maxV, float alpha) {
        boolean isDashed = (type == RenderType.DRAFT || type == RenderType.CONFLICT);
        RenderProperties props = RenderProperties.fromColor(color, alpha, isDashed);
        double inset = RegionRenderConfig.INSET;

        return switch (dir) {
            case DOWN, UP -> new RegionFace(type, dir, minU, fixedCoord, minV, maxU, fixedCoord, maxV,
                    minU, minV, maxU, maxV, inset, true, props);

            case NORTH, SOUTH -> new RegionFace(type, dir, minU, minV, fixedCoord, maxU, maxV, fixedCoord,
                    minU, minV, maxU, maxV, inset, true, props);

            case WEST, EAST -> new RegionFace(type, dir, fixedCoord, minV, minU, fixedCoord, maxV, maxU,
                    minU, minV, maxU, maxV, inset, true, props);
        };
    }

    public RenderProperties getRenderProperties() { return this.renderProperties; }

    public boolean isEmpty() { return minU >= maxU || minV >= maxV; }

    public boolean intersects(RegionFace other) {
        if (this.faceDirection != other.faceDirection) return false;
        return !(maxU <= other.minU || minU >= other.maxU ||
                maxV <= other.minV || minV >= other.maxV);
    }

    public double minU() { return minU; }
    public double minV() { return minV; }
    public double maxU() { return maxU; }
    public double maxV() { return maxV; }
}