package dev.slarrties.privit.client.render.face;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.render.state.RenderPriority;

import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Map;
import java.util.EnumMap;

public final class FaceLayers {
    private final Map<RenderPriority, RenderType[]> layersCache = new EnumMap<>(RenderPriority.class);
    private final AnimationController animationController = new AnimationController();

    public FaceLayers() { initializeAssets(); }

    public RenderType getLayer(RenderPriority type) {
        RenderType[] layers = layersCache.get(type);
        if (layers == null || layers.length == 0) return layersCache.get(RenderPriority.ORIGINAL)[0];
        int index = animationController.getCurrentFrameIndex(type);
        return layers[Math.max(0, Math.min(index, layers.length - 1))];
    }

    private void initializeAssets() {
        Identifier originalTex = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/region/real.png");
        layersCache.put(RenderPriority.ORIGINAL, new RenderType[]{ createFillLayer("original", originalTex) });

        for (RenderPriority type : new RenderPriority[]{RenderPriority.DRAFT, RenderPriority.CONFLICT}) {
            int frameCount = animationController.getFrameCount(type);
            RenderType[] layers = new RenderType[frameCount];
            String subfolder = type.name().toLowerCase() + "_animated";
            for (int i = 1; i <= frameCount; i++) {
                Identifier tex = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID,
                        "textures/region/" + subfolder + "/" + i + ".png");
                layers[i - 1] = createFillLayer(type.name().toLowerCase() + "_" + i, tex);
            }
            layersCache.put(type, layers);
        }
    }

    private static RenderType createFillLayer(String suffix, Identifier texture) {
        RenderSetup setup = RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
                .withTexture("Sampler0", texture)
                .bufferSize(786432)
                .createRenderSetup();
        return RenderType.create("privit:region_fill_" + suffix, setup);
    }

    private static final class AnimationController {

        private record Config(int frameCount, int frameDurationMs) {}

        private final Map<RenderPriority, Config> configs = new EnumMap<>(RenderPriority.class);
        private final long startTime = System.currentTimeMillis();

        public AnimationController() {
            configs.put(RenderPriority.DRAFT, new Config(16, 150));
            configs.put(RenderPriority.CONFLICT, new Config(16, 150));
        }

        public int getCurrentFrameIndex(RenderPriority type) {
            Config c = configs.get(type);
            if (c == null) return 0;
            long time = System.currentTimeMillis() - startTime;
            return (int) ((time / c.frameDurationMs) % c.frameCount);
        }

        public int getFrameCount(RenderPriority type) {
            Config c = configs.get(type);
            return c == null ? 1 : c.frameCount;
        }
    }
}