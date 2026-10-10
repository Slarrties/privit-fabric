package dev.slarrties.privit.client.render.edge;

import dev.slarrties.privit.client.render.state.RegionRenderEntry;
import net.minecraft.util.math.BlockBox;
import java.util.*;

public final class EdgeCompiler {

    private record NormalizedEdge(double x1, double y1, double z1, double x2, double y2, double z2) {
        public NormalizedEdge(Edge e) {
            this(Math.min(e.x1(), e.x2()), Math.min(e.y1(), e.y2()), Math.min(e.z1(), e.z2()),
                    Math.max(e.x1(), e.x2()), Math.max(e.y1(), e.y2()), Math.max(e.z1(), e.z2()));
        }
    }

    private EdgeCompiler() {}

    public static EdgeMesh compile(RegionRenderEntry entry) {
        List<Edge> solid = new ArrayList<>();
        List<Edge> dashed = new ArrayList<>();
        Set<NormalizedEdge> drawn = new HashSet<>();

        for (BlockBox box : entry.conflicts()) addBoxEdges(box, true, dashed, drawn);
        if (entry.hasOriginal()) addBoxEdgesClipped(entry.original(), false, entry.conflicts(), solid, drawn);
        if (entry.hasDraft()) addBoxEdgesClipped(entry.draft(), true, entry.conflicts(), dashed, drawn);

        return new EdgeMesh(solid, dashed);
    }

    private static void addBoxEdges(BlockBox box, boolean dashed, List<Edge> target, Set<NormalizedEdge> drawn) {
        if (box == null) return;
        for (Edge e : getBoxEdges(box)) {
            NormalizedEdge ne = new NormalizedEdge(e);
            if (drawn.add(ne)) target.add(e);
        }
    }

    private static void addBoxEdgesClipped(BlockBox box, boolean dashed, List<BlockBox> conflicts,
                                           List<Edge> target, Set<NormalizedEdge> drawn) {
        if (box == null) return;

        for (Edge edge : getBoxEdges(box)) {
            List<Edge> currentSegments = new ArrayList<>();
            currentSegments.add(edge);

            for (BlockBox c : conflicts) {
                List<Edge> nextSegments = new ArrayList<>();

                for (Edge seg : currentSegments) nextSegments.addAll(subtractEdgeFromBox(seg, c));

                currentSegments = nextSegments;
                if (currentSegments.isEmpty()) break;
            }

            for (Edge seg : currentSegments) {
                NormalizedEdge ne = new NormalizedEdge(seg);
                if (drawn.add(ne)) target.add(seg);
            }
        }
    }

    private static List<Edge> subtractEdgeFromBox(Edge seg, BlockBox b) {
        double bMinX = b.getMinX(), bMaxX = b.getMaxX() + 1.0;
        double bMinY = b.getMinY(), bMaxY = b.getMaxY() + 1.0;
        double bMinZ = b.getMinZ(), bMaxZ = b.getMaxZ() + 1.0;

        if (seg.y1() == seg.y2() && seg.z1() == seg.z2()) {
            if (seg.y1() < bMinY || seg.y1() > bMaxY || seg.z1() < bMinZ || seg.z1() > bMaxZ) return List.of(seg);
            return subtract1D(seg.x1(), seg.x2(), bMinX, bMaxX, seg.y1(), seg.z1(), 'X');
        } else if (seg.x1() == seg.x2() && seg.z1() == seg.z2()) {
            if (seg.x1() < bMinX || seg.x1() > bMaxX || seg.z1() < bMinZ || seg.z1() > bMaxZ) return List.of(seg);
            return subtract1D(seg.y1(), seg.y2(), bMinY, bMaxY, seg.x1(), seg.z1(), 'Y');
        } else if (seg.x1() == seg.x2() && seg.y1() == seg.y2()) {
            if (seg.x1() < bMinX || seg.x1() > bMaxX || seg.y1() < bMinY || seg.y1() > bMaxY) return List.of(seg);
            return subtract1D(seg.z1(), seg.z2(), bMinZ, bMaxZ, seg.x1(), seg.y1(), 'Z');
        }

        return List.of(seg);
    }

    private static List<Edge> subtract1D(double v1, double v2, double cutMin, double cutMax,
                                         double fixed1, double fixed2, char axis) {
        double minV = Math.min(v1, v2);
        double maxV = Math.max(v1, v2);
        double interMin = Math.max(minV, cutMin);
        double interMax = Math.min(maxV, cutMax);
        if (interMin >= interMax) return List.of(createEdge(v1, v2, fixed1, fixed2, axis));

        List<Edge> result = new ArrayList<>();
        if (minV < interMin) result.add(createEdge(minV, interMin, fixed1, fixed2, axis));
        if (maxV > interMax) result.add(createEdge(interMax, maxV, fixed1, fixed2, axis));

        return result;
    }

    private static Edge createEdge(double v1, double v2, double fixed1, double fixed2, char axis) {
        return switch (axis) {
            case 'X' -> new Edge(v1, fixed1, fixed2, v2, fixed1, fixed2);
            case 'Y' -> new Edge(fixed1, v1, fixed2, fixed1, v2, fixed2);
            case 'Z' -> new Edge(fixed1, fixed2, v1, fixed1, fixed2, v2);
            default -> throw new IllegalArgumentException("Unknown axis: " + axis);
        };
    }

    private static Edge[] getBoxEdges(BlockBox box) {
        double minX = box.getMinX(), minY = box.getMinY(), minZ = box.getMinZ();
        double maxX = box.getMaxX() + 1.0, maxY = box.getMaxY() + 1.0, maxZ = box.getMaxZ() + 1.0;

        return new Edge[]{
                new Edge(minX, minY, minZ, maxX, minY, minZ), new Edge(maxX, minY, minZ, maxX, minY, maxZ),
                new Edge(maxX, minY, maxZ, minX, minY, maxZ), new Edge(minX, minY, maxZ, minX, minY, minZ),
                new Edge(minX, maxY, minZ, maxX, maxY, minZ), new Edge(maxX, maxY, minZ, maxX, maxY, maxZ),
                new Edge(maxX, maxY, maxZ, minX, maxY, maxZ), new Edge(minX, maxY, maxZ, minX, maxY, minZ),
                new Edge(minX, minY, minZ, minX, maxY, minZ), new Edge(maxX, minY, minZ, maxX, maxY, minZ),
                new Edge(maxX, minY, maxZ, maxX, maxY, maxZ), new Edge(minX, minY, maxZ, minX, maxY, maxZ)
        };
    }
}