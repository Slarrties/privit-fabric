package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.client.render.state.RenderType;

import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Objects;

public record FaceMesh(RenderType type, List<Face> faces, Box boundingBox) {
    public FaceMesh {
        Objects.requireNonNull(type);
        faces = List.copyOf(faces);
        boundingBox = computeBoundingBox(faces);
    }

    public static FaceMesh of(RenderType type, List<Face> faces) { return new FaceMesh(type, faces, null); }

    public boolean isEmpty() { return faces.isEmpty(); }

    private static Box computeBoundingBox(List<Face> faces) {
        if (faces.isEmpty()) return new Box(0, 0, 0, 0, 0, 0);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (Face f : faces) {
            minX = Math.min(minX, f.minX());
            minY = Math.min(minY, f.minY());
            minZ = Math.min(minZ, f.minZ());
            maxX = Math.max(maxX, f.maxX());
            maxY = Math.max(maxY, f.maxY());
            maxZ = Math.max(maxZ, f.maxZ());
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}