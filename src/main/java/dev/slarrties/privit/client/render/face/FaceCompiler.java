package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.client.render.state.RenderType;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;
import dev.slarrties.privit.client.render.config.RegionRenderConfig;
import dev.slarrties.privit.common.region.Color;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;

import java.util.*;

public final class FaceCompiler {

    private record PlaneKey(Direction direction, double fixedCoord) {}

    private FaceCompiler() {}

    public static List<FaceMesh> compile(List<RegionRenderEntry> entries) {
        if (entries.isEmpty()) return List.of();
        Map<PlaneKey, List<Face>> planes = collectAllFaces(entries);
        List<Face> finalFaces = new ArrayList<>();

        for (List<Face> planeFaces : planes.values()) {
            planeFaces.sort(Comparator.comparingInt((Face f) -> f.type().getPriority()).reversed());
            finalFaces.addAll(computeVisibleOnPlane(planeFaces));
        }

        return groupIntoMeshes(finalFaces);
    }

    private static Map<PlaneKey, List<Face>> collectAllFaces(List<RegionRenderEntry> entries) {
        Map<PlaneKey, List<Face>> planes = new HashMap<>();

        for (RegionRenderEntry entry : entries) {
            Color color = entry.color();

            for (BlockBox box : entry.conflicts()) addFacesFromBox(planes, box, RenderType.CONFLICT, color, RegionRenderConfig.ALPHA_CONFLICT);
            if (entry.hasOriginal()) addFacesFromBox(planes, entry.original(), RenderType.ORIGINAL, color, RegionRenderConfig.ALPHA_ORIGINAL);
            if (entry.hasDraft()) addFacesFromBox(planes, entry.draft(), RenderType.DRAFT, color, RegionRenderConfig.ALPHA_DRAFT);
        }
        return planes;
    }

    private static void addFacesFromBox(Map<PlaneKey, List<Face>> planes, BlockBox box, RenderType type, Color color, float alpha) {
        if (box == null) return;

        double minX = box.getMinX(), maxX = box.getMaxX() + 1.0, minY = box.getMinY(), maxY = box.getMaxY() + 1.0, minZ = box.getMinZ(), maxZ = box.getMaxZ() + 1.0;
        addFace(planes, Direction.DOWN, minY, type, color, alpha, minX, minZ, maxX, maxZ);
        addFace(planes, Direction.UP, maxY, type, color, alpha, minX, minZ, maxX, maxZ);
        addFace(planes, Direction.NORTH, minZ, type, color, alpha, minX, minY, maxX, maxY);
        addFace(planes, Direction.SOUTH, maxZ, type, color, alpha, minX, minY, maxX, maxY);
        addFace(planes, Direction.WEST, minX, type, color, alpha, minZ, minY, maxZ, maxY);
        addFace(planes, Direction.EAST, maxX, type, color, alpha, minZ, minY, maxZ, maxY);
    }

    private static void addFace(Map<PlaneKey, List<Face>> planes, Direction dir, double fixed, RenderType type, Color color, float alpha, double minU, double minV, double maxU, double maxV) {
        planes.computeIfAbsent(new PlaneKey(dir, fixed), k -> new ArrayList<>()).add(Face.fromPlane(type, dir, fixed, minU, minV, maxU, maxV, color, alpha));
    }

    private static List<Face> computeVisibleOnPlane(List<Face> allFaces) {
        List<Face> visible = new ArrayList<>();

        for (int i = 0; i < allFaces.size(); i++) {
            Face current = allFaces.get(i);
            List<Face> remaining = subtractAll(current, allFaces.subList(0, i));
            visible.addAll(remaining);
        }
        return visible;
    }

    private static List<Face> subtractAll(Face subject, List<Face> obstacles) {
        List<Face> current = List.of(subject);

        for (Face obstacle : obstacles) {
            List<Face> next = new ArrayList<>();
            for (Face piece : current) next.addAll(subtract(piece, obstacle));
            current = next;
            if (current.isEmpty()) break;
        }

        return current;
    }

    private static List<Face> subtract(Face subject, Face obstacle) {
        if (!subject.intersects(obstacle)) return List.of(subject);
        if (obstacle.minU() <= subject.minU() && obstacle.maxU() >= subject.maxU() && obstacle.minV() <= subject.minV() && obstacle.maxV() >= subject.maxV()) return List.of();

        List<Face> result = new ArrayList<>();
        double sMinU = subject.minU(), sMaxU = subject.maxU(), sMinV = subject.minV(), sMaxV = subject.maxV();
        double oMinU = obstacle.minU(), oMaxU = obstacle.maxU(), oMinV = obstacle.minV(), oMaxV = obstacle.maxV();

        if (sMinU < oMinU) result.add(createRemaining(subject, sMinU, sMinV, oMinU, sMaxV));
        if (sMaxU > oMaxU) result.add(createRemaining(subject, oMaxU, sMinV, sMaxU, sMaxV));
        if (sMinV < oMinV && sMaxU > oMinU && sMinU < oMaxU) result.add(createRemaining(subject, Math.max(sMinU, oMinU), sMinV, Math.min(sMaxU, oMaxU), oMinV));
        if (sMaxV > oMaxV && sMaxU > oMinU && sMinU < oMaxU) result.add(createRemaining(subject, Math.max(sMinU, oMinU), oMaxV, Math.min(sMaxU, oMaxU), sMaxV));

        return result;
    }

    private static Face createRemaining(Face original, double minU, double minV, double maxU, double maxV) {
        double fixed = switch (original.faceDirection().getAxis()) {
            case X -> original.faceDirection() == Direction.WEST ? original.minX() : original.maxX();
            case Y -> original.faceDirection() == Direction.DOWN ? original.minY() : original.maxY();
            case Z -> original.faceDirection() == Direction.NORTH ? original.minZ() : original.maxZ();
        };

        return Face.fromPlane(original.type(), original.faceDirection(), fixed, minU, minV, maxU, maxV, original.color(), original.baseAlpha());
    }

    private static List<FaceMesh> groupIntoMeshes(List<Face> faces) {
        Map<RenderType, List<Face>> byType = new EnumMap<>(RenderType.class);
        for (Face face : faces) byType.computeIfAbsent(face.type(), k -> new ArrayList<>()).add(face);
        return byType.entrySet().stream().map(e -> FaceMesh.of(e.getKey(), e.getValue())).toList();
    }
}