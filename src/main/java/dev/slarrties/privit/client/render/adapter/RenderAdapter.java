package dev.slarrties.privit.client.render.adapter;

import dev.slarrties.privit.client.render.face.FaceMesh;
import dev.slarrties.privit.client.render.state.RegionRenderEntry;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import java.util.List;

public interface RenderAdapter {
    void renderFaces(List<FaceMesh> meshes, WorldRenderContext context);
    void renderEdges(List<RegionRenderEntry> entries, WorldRenderContext context);
}