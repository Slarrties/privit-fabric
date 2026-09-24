package dev.slarrties.privit.client.gui.widget;

import net.minecraft.util.Identifier;

public record ButtonTextures(Identifier enabled, Identifier disabled, Identifier hovered) {
    public Identifier get(boolean active, boolean hovered) {
        if (!active) return this.disabled;
        if (hovered) return this.hovered;
        return this.enabled;
    }
}