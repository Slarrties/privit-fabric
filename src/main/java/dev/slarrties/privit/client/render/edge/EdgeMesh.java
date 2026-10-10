package dev.slarrties.privit.client.render.edge;

import java.util.List;

public record EdgeMesh(List<Edge> solid, List<Edge> dashed) {
    public static final EdgeMesh EMPTY = new EdgeMesh(List.of(), List.of());
}