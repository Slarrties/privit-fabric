package dev.slarrties.privit.client.render;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.render.animation.AnimationRegistry;

import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public final class RegionRenderLayers {

    public static final RenderLayer ORIGINAL = createFillLayer("privit:original_fill",
            Identifier.of(PrivitMod.MOD_ID, "textures/region/real.png"));

    public static final RenderLayer CONFLICT = createFillLayer("privit:conflict_fill",
            Identifier.of(PrivitMod.MOD_ID, "textures/region/conflict.png"));

    public static final RenderLayer EDGE_LAYER = RenderLayer.of(
            "privit:region_edge_lines",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.LINES,
            32768,
            false, false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(java.util.OptionalDouble.of(2.5)))
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    public static RenderLayer getDraftLayer() {
        Identifier frame = AnimationRegistry.getInstance()
                .getAnimation(RenderType.DRAFT)
                .getCurrentFrame();
        return createFillLayer("privit:draft_fill_" + frame.getPath(), frame);
    }

    private static RenderLayer createFillLayer(String name, Identifier texture) {
        return RenderLayer.of(
                name,
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                786432,
                true, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.TRANSLUCENT_PROGRAM)
                        .texture(new RenderPhase.Texture(texture, false, true))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(false)
        );
    }

    private RegionRenderLayers() {}
}