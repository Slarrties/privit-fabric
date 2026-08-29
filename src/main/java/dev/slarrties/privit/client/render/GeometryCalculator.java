package dev.slarrties.privit.client.render;

import dev.slarrties.privit.common.region.Color;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

import java.util.*;

public final class GeometryCalculator {

    private GeometryCalculator() {}

    public static List<RegionGeometry> computeAll(List<RegionRenderEntry> entries) {
        if (entries.isEmpty()) return List.of();

        Map<PlaneKey, List<RegionFace>> planes = collectAllFaces(entries);
        List<RegionFace> finalFaces = new ArrayList<>();

        for (List<RegionFace> planeFaces : planes.values()) {
            planeFaces.sort((a, b) -> RenderType.compare(a.type(), b.type()));
            List<RegionFace> visible = computeVisibleOnPlane(planeFaces);
            finalFaces.addAll(visible);
        }

        return groupIntoGeometries(finalFaces);
    }

    private static Map<PlaneKey, List<RegionFace>> collectAllFaces(List<RegionRenderEntry> entries) {
        Map<PlaneKey, List<RegionFace>> planes = new HashMap<>();

        for (RegionRenderEntry entry : entries) {
            Color color = entry.color();
            float baseAlpha = getAlpha(entry);

            for (BlockBox box : entry.conflicts()) {
                addFacesFromBox(planes, box, RenderType.CONFLICT, color,
                        baseAlpha * RegionRenderConfig.CONFLICT_ALPHA_MULTIPLIER);
            }

            if (entry.hasOriginal()) {
                addFacesFromBox(planes, entry.original(), RenderType.ORIGINAL, color,
                        RegionRenderConfig.ALPHA_ORIGINAL);
            }

            if (entry.hasDraft()) {
                addFacesFromBox(planes, entry.draft(), RenderType.DRAFT, color,
                        RegionRenderConfig.ALPHA_DRAFT);
            }
        }
        return planes;
    }

    private static void addFacesFromBox(Map<PlaneKey, List<RegionFace>> planes, BlockBox box,
                                        RenderType type, Color color, float alpha) {
        if (box == null) return;

        double minX = box.getMinX(), maxX = box.getMaxX() + 1.0;
        double minY = box.getMinY(), maxY = box.getMaxY() + 1.0;
        double minZ = box.getMinZ(), maxZ = box.getMaxZ() + 1.0;

        addFace(planes, Direction.DOWN,  minY, type, color, alpha, minX, minZ, maxX, maxZ);
        addFace(planes, Direction.UP,    maxY, type, color, alpha, minX, minZ, maxX, maxZ);
        addFace(planes, Direction.NORTH, minZ, type, color, alpha, minX, minY, maxX, maxY);
        addFace(planes, Direction.SOUTH, maxZ, type, color, alpha, minX, minY, maxX, maxY);
        addFace(planes, Direction.WEST,  minX, type, color, alpha, minZ, minY, maxZ, maxY);
        addFace(planes, Direction.EAST,  maxX, type, color, alpha, minZ, minY, maxZ, maxY);
    }

    private static void addFace(Map<PlaneKey, List<RegionFace>> planes, Direction dir, double fixed,
                                RenderType type, Color color, float alpha,
                                double minU, double minV, double maxU, double maxV) {
        PlaneKey key = new PlaneKey(dir, fixed);
        planes.computeIfAbsent(key, k -> new ArrayList<>())
                .add(RegionFace.fromBoxEntry(type, color, dir, fixed, minU, minV, maxU, maxV, alpha));
    }

    // ====================== Boolean difference ======================

    private static List<RegionFace> computeVisibleOnPlane(List<RegionFace> allFaces) {
        List<RegionFace> visible = new ArrayList<>();

        for (int i = 0; i < allFaces.size(); i++) {
            RegionFace current = allFaces.get(i);
            List<RegionFace> obstacles = allFaces.subList(0, i);

            List<RegionFace> remaining = subtractAll(current, obstacles);
            visible.addAll(remaining);
        }
        return visible;
    }

    private static List<RegionFace> subtractAll(RegionFace subject, List<RegionFace> obstacles) {
        List<RegionFace> current = List.of(subject);

        for (RegionFace obstacle : obstacles) {
            List<RegionFace> next = new ArrayList<>();
            for (RegionFace piece : current) {
                next.addAll(subtract(piece, obstacle));
            }
            current = next;
            if (current.isEmpty()) break;
        }
        return current;
    }

    private static List<RegionFace> subtract(RegionFace subject, RegionFace obstacle) {
        if (!subject.intersects(obstacle)) {
            return List.of(subject);
        }

        if (isFullyCovered(subject, obstacle)) {
            return List.of();
        }

        List<RegionFace> result = new ArrayList<>();
        double sMinU = subject.minU(), sMaxU = subject.maxU();
        double sMinV = subject.minV(), sMaxV = subject.maxV();
        double oMinU = obstacle.minU(), oMaxU = obstacle.maxU();
        double oMinV = obstacle.minV(), oMaxV = obstacle.maxV();

        if (sMinU < oMinU) result.add(createRemainingFace(subject, sMinU, sMinV, oMinU, sMaxV));
        if (sMaxU > oMaxU) result.add(createRemainingFace(subject, oMaxU, sMinV, sMaxU, sMaxV));
        if (sMinV < oMinV && sMaxU > oMinU && sMinU < oMaxU)
            result.add(createRemainingFace(subject, Math.max(sMinU, oMinU), sMinV, Math.min(sMaxU, oMaxU), oMinV));
        if (sMaxV > oMaxV && sMaxU > oMinU && sMinU < oMaxU)
            result.add(createRemainingFace(subject, Math.max(sMinU, oMinU), oMaxV, Math.min(sMaxU, oMaxU), sMaxV));

        return result;
    }

    private static RegionFace createRemainingFace(RegionFace original,
                                                  double minU, double minV, double maxU, double maxV) {
        return RegionFace.from2D(
                original.type(),
                original.faceDirection(),
                getFixedCoord(original),
                minU, minV, maxU, maxV,
                original.renderProperties()
        );
    }

    private static boolean isFullyCovered(RegionFace subject, RegionFace obstacle) {
        return obstacle.minU() <= subject.minU() && obstacle.maxU() >= subject.maxU() &&
                obstacle.minV() <= subject.minV() && obstacle.maxV() >= subject.maxV();
    }

    private static double getFixedCoord(RegionFace face) {
        return switch (face.faceDirection().getAxis()) {
            case X -> face.faceDirection() == Direction.WEST ? face.minX() : face.maxX();
            case Y -> face.faceDirection() == Direction.DOWN ? face.minY() : face.maxY();
            case Z -> face.faceDirection() == Direction.NORTH ? face.minZ() : face.maxZ();
        };
    }

    private static List<RegionGeometry> groupIntoGeometries(List<RegionFace> faces) {
        Map<RenderType, List<RegionFace>> byType = new EnumMap<>(RenderType.class);

        for (RegionFace face : faces) {
            byType.computeIfAbsent(face.type(), k -> new ArrayList<>()).add(face);
        }

        return byType.entrySet().stream()
                .map(e -> RegionGeometry.of(e.getKey(), e.getValue()))
                .toList();
    }

    private static float getAlpha(RegionRenderEntry entry) {
        if (!entry.conflicts().isEmpty()) return RegionRenderConfig.ALPHA_CONFLICT;
        if (entry.hasDraft()) return RegionRenderConfig.ALPHA_DRAFT;
        return RegionRenderConfig.ALPHA_ORIGINAL;
    }
}