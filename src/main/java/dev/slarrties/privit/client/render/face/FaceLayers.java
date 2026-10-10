package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.render.state.RenderType;

import net.minecraft.util.Util;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.Map;
import java.util.EnumMap;

public final class FaceLayers {

    private final Map<RenderType, RenderLayer[]> layersCache = new EnumMap<>(RenderType.class);
    private final Map<RenderType, Identifier[]> texturesCache = new EnumMap<>(RenderType.class);
    private final AnimationController animationController = new AnimationController();

    public FaceLayers() { initializeAssets(); }

    public RenderLayer getLayer(RenderType type) { return layersCache.get(type)[animationController.getCurrentFrameIndex(type)]; }

    public Identifier getTexture(RenderType type) { return texturesCache.get(type)[animationController.getCurrentFrameIndex(type)]; }

    private void initializeAssets() {
        Identifier originalTex = Identifier.of(PrivitMod.MOD_ID, "textures/region/real.png");
        texturesCache.put(RenderType.ORIGINAL, new Identifier[]{ originalTex });
        layersCache.put(RenderType.ORIGINAL, new RenderLayer[]{ createFillLayer("original", originalTex) });

        for (RenderType type : new RenderType[]{RenderType.DRAFT, RenderType.CONFLICT}) {
            int frameCount = animationController.getFrameCount(type);
            RenderLayer[] layers = new RenderLayer[frameCount];
            Identifier[] textures = new Identifier[frameCount];
            String subfolder = type.name().toLowerCase() + "_animated";

            for (int i = 1; i <= frameCount; i++) {
                Identifier tex = Identifier.of(PrivitMod.MOD_ID, "textures/region/" + subfolder + "/" + i + ".png");
                textures[i - 1] = tex;
                layers[i - 1] = createFillLayer(type.name().toLowerCase() + "_" + i, tex);
            }

            texturesCache.put(type, textures);
            layersCache.put(type, layers);
        }
    }

    private static RenderLayer createFillLayer(String suffix, Identifier texture) {
        return RenderLayer.of("privit:region_fill_" + suffix, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 786432, true, true,
                RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.TRANSLUCENT_PROGRAM)
                        .texture(new RenderPhase.Texture(texture, false, true))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(false));
    }

    private static final class AnimationController {

        private record Config(int frameCount, int frameDurationMs) {}

        private final Map<RenderType, Config> configs = new EnumMap<>(RenderType.class);
        private final long startTime = Util.getMeasuringTimeMs();

        public AnimationController() {
            configs.put(RenderType.DRAFT, new Config(16, 150));
            configs.put(RenderType.CONFLICT, new Config(16, 150));
        }

        public int getCurrentFrameIndex(RenderType type) {
            Config c = configs.get(type);
            if (c == null) return 0;
            long time = Util.getMeasuringTimeMs() - startTime;
            return (int) Math.floorMod(time / c.frameDurationMs, c.frameCount);
        }

        public int getFrameCount(RenderType type) { Config c = configs.get(type); return c == null ? 1 : c.frameCount; }
    }
}