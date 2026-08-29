package dev.slarrties.privit.client.render.animation;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.render.RenderType;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.EnumMap;

public final class AnimationRegistry {

    private static final AnimationRegistry INSTANCE = new AnimationRegistry();
    private final Map<RenderType, TextureAnimation> animations = new EnumMap<>(RenderType.class);

    public static AnimationRegistry getInstance() {
        return INSTANCE;
    }

    private AnimationRegistry() {
        animations.put(RenderType.DRAFT, new DraftAnimation());
    }

    public TextureAnimation getAnimation(RenderType type) {
        return animations.getOrDefault(type, StaticTexture.DEFAULT);
    }

    public void resetAll() {
        animations.values().forEach(TextureAnimation::reset);
    }

    private static final class StaticTexture implements TextureAnimation {
        private static final StaticTexture DEFAULT = new StaticTexture();
        private static final Identifier ORIGINAL = Identifier.of(PrivitMod.MOD_ID, "textures/region/real.png");
        private static final Identifier CONFLICT = Identifier.of(PrivitMod.MOD_ID, "textures/region/conflict.png");

        @Override
        public Identifier getCurrentFrame() {
            return RenderType.ORIGINAL == null ? ORIGINAL : CONFLICT;
        }

        @Override
        public void reset() {}

        @Override
        public boolean isAnimated() { return false; }
    }
}