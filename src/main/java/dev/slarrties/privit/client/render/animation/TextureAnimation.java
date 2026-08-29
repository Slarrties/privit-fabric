package dev.slarrties.privit.client.render.animation;

import net.minecraft.util.Identifier;

public interface TextureAnimation {

    Identifier getCurrentFrame();
    void reset();
    boolean isAnimated();
}