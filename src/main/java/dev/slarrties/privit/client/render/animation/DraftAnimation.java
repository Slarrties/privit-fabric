package dev.slarrties.privit.client.render.animation;

import dev.slarrties.privit.PrivitMod;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

public final class DraftAnimation implements TextureAnimation {

    private static final int FRAME_DURATION_MS = 150;
    private final List<Identifier> frames = new ArrayList<>();
    private long animationStartTime = 0;

    public DraftAnimation() {
        loadFrames();
    }

    private void loadFrames() {
        frames.clear();
        for (int i = 1; i <= 16; i++) {
            frames.add(Identifier.of(PrivitMod.MOD_ID, "textures/region/draft_animated/" + i + ".png"));
        }
    }

    @Override
    public Identifier getCurrentFrame() {
        if (frames.isEmpty()) return Identifier.of(PrivitMod.MOD_ID, "textures/region/real.png");

        long time = System.currentTimeMillis() - animationStartTime;
        int index = (int) ((time / FRAME_DURATION_MS) % frames.size());
        return frames.get(index);
    }

    @Override
    public void reset() {
        animationStartTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAnimated() {
        return true;
    }
}