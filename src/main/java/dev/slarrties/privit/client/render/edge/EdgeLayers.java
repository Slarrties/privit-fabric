package dev.slarrties.privit.client.render.edge;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class EdgeLayers {
    private static final Identifier WHITE_TEX = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/region/white.png");
    private static final RenderSetup EDGE_SETUP =
            RenderSetup.builder(RenderPipelines.TEXT_SEE_THROUGH)
                    .withTexture("Sampler0", WHITE_TEX)
                    .bufferSize(786432)
                    .createRenderSetup();
    public static final RenderType EDGE_QUADS = RenderType.create("privit:region_edge_quads", EDGE_SETUP);

    private EdgeLayers() {}
}