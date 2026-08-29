package dev.slarrties.privit.client.render;

import net.minecraft.util.math.BlockBox;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

public final class RegionGeometry {

    private final RenderType type;
    private final List<RegionFace> faces;

    private RegionGeometry(RenderType type, List<RegionFace> faces) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.faces = List.copyOf(Objects.requireNonNull(faces, "faces cannot be null"));
    }

    public static RegionGeometry fromBox(BlockBox box, RenderType type, dev.slarrties.privit.common.region.Color color, float alpha) {
        if (box == null) return new RegionGeometry(type, List.of());

        List<RegionFace> faceList = RegionFace.fromBox(box, type, color, alpha);
        return new RegionGeometry(type, faceList);
    }

    public static RegionGeometry of(RenderType type, List<RegionFace> faces) {
        return new RegionGeometry(type, faces);
    }

    public static RegionGeometry empty(RenderType type) {
        return new RegionGeometry(type, List.of());
    }

    public RenderType getType() { return type; }

    public List<RegionFace> getFaces() { return faces; }

    public boolean isEmpty() { return faces.isEmpty(); }

    public RegionGeometry merge(RegionGeometry other) {
        if (this.type != other.type)
            throw new IllegalArgumentException("Cannot merge geometries with different types: " + this.type + " and " + other.type);
        if (other.isEmpty()) return this;
        if (this.isEmpty()) return other;

        List<RegionFace> combined = new ArrayList<>(this.faces);
        combined.addAll(other.faces);

        return new RegionGeometry(this.type, combined);
    }

    public int faceCount() { return faces.size(); }

    @Override
    public String toString() {
        return "RegionGeometry{" +
                "type=" + type +
                ", faces=" + faces.size() +
                '}';
    }
}