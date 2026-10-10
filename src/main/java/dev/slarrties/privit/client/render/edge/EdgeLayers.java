package dev.slarrties.privit.client.render.edge;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.OptionalDouble;

public final class EdgeLayers {

    public static final RenderLayer LINE_LAYER = RenderLayer.of(
            "privit:region_edge_lines", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 1536, false, false,
            RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.5)))
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .build(false)
    );

    private EdgeLayers() {}
}