package dev.slarrties.privit.client.hud;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.config.sections.HudSection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import java.util.Objects;

public final class RegionNameHudOverlay implements HudElement {
    public static final RegionNameHudOverlay INSTANCE = new RegionNameHudOverlay();

    private static final String CUBE_SYMBOL = "\uE000";
    private static final Identifier CUSTOM_FONT_ID = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "default");
    private static final FontDescription CUSTOM_FONT = new FontDescription.Resource(CUSTOM_FONT_ID);

    private static final long APPEAR_DURATION = 800L;
    private static final long HOLD_DURATION = 4000L;
    private static final long FADE_DURATION = 800L;

    private String regionName = null;
    private Color regionColor = Color.WHITE;
    private long appearStart = 0L;
    private long fadeStart = 0L;

    private MutableComponent cachedNameText = null;
    private MutableComponent cachedCubeText = null;
    private int cachedNameWidth = 0;
    private int cachedCubeWidth = 0;

    private RegionNameHudOverlay() {}

    public void update(String newName, Color newColor) {
        if (newName != null && newName.isBlank()) newName = null;
        int newRgb = newColor != null ? newColor.getColorValue() : -1;
        int oldRgb = regionColor != null ? regionColor.getColorValue() : -1;
        boolean changed = !Objects.equals(newName, regionName) || newRgb != oldRgb;

        if (changed) {
            this.regionName = newName;
            if (this.regionName != null) this.regionColor = newColor;
            long now = System.currentTimeMillis();
            this.appearStart = now;
            this.fadeStart = newName != null ? now + HOLD_DURATION : 0;
            precomputeTexts();
        }
    }

    private void precomputeTexts() {
        Minecraft minecraft = Minecraft.getInstance();

        if (regionName == null) {
            int rgb = regionColor != null ? regionColor.getColorValue() : 0xFFFFFF;
            this.cachedNameText = Component.translatable("privit.hud.outside_region")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
            this.cachedCubeText = Component.literal(CUBE_SYMBOL)
                    .withStyle(s -> s.withFont(CUSTOM_FONT).withColor(TextColor.fromRgb(rgb)));
        } else {
            int rgb = regionColor.getColorValue();
            this.cachedNameText = Component.literal(regionName)
                    .withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
            this.cachedCubeText = Component.literal(CUBE_SYMBOL)
                    .withStyle(s -> s.withFont(CUSTOM_FONT).withColor(TextColor.fromRgb(rgb)));
        }

        this.cachedNameWidth = minecraft.font.width(cachedNameText);
        this.cachedCubeWidth = minecraft.font.width(cachedCubeText);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (minecraft.options.hideGui) return;
        if (cachedNameText == null) {
            precomputeTexts();
            if (cachedNameText == null) return;
        }

        long now = System.currentTimeMillis();
        if (regionName == null) {
            long elapsed = now - appearStart;
            if (elapsed >= FADE_DURATION) return;
            float alpha = 1.0f - Math.min(elapsed, FADE_DURATION) / (float) FADE_DURATION;
            if (alpha < 0.05f) return;
            renderOutsideText(graphics, alpha);
            return;
        }

        long appearElapsed = now - appearStart;
        long fadeElapsed = now - fadeStart;

        float appearAlpha = appearElapsed < APPEAR_DURATION
                ? Math.min(appearElapsed / (float) APPEAR_DURATION, 1.0f)
                : 1.0f;
        if (appearAlpha < 0.05f) return;

        float nameAlpha = fadeElapsed > 0
                ? Math.max(1.0f - fadeElapsed / (float) FADE_DURATION, 0.0f)
                : 1.0f;
        if (nameAlpha < 0.07f) nameAlpha = 0.0f;

        HudCoords coords = calculateCoords(graphics, cachedNameWidth, cachedCubeWidth);

        if (nameAlpha > 0.0f) {
            int nameColor = ((int) (nameAlpha * appearAlpha * 255F) << 24) | 0xFFFFFF;
            graphics.text(minecraft.font, cachedNameText, coords.textX(), coords.y(), nameColor, true);
        }

        if (appearAlpha > 0.0f) {
            int cubeColor = ((int) (appearAlpha * 255F) << 24) | 0xFFFFFF;
            graphics.text(minecraft.font, cachedCubeText, coords.cubeX(), coords.y(), cubeColor, true);
        }
    }

    private void renderOutsideText(GuiGraphicsExtractor graphics, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        HudCoords coords = calculateCoords(graphics, cachedNameWidth, cachedCubeWidth);
        int finalColor = ((int) (alpha * 255F) << 24) | 0xFFFFFF;

        graphics.text(minecraft.font, cachedNameText, coords.textX(), coords.y(), finalColor, true);
        graphics.text(minecraft.font, cachedCubeText, coords.cubeX(), coords.y(), finalColor, true);
    }

    private record HudCoords(int textX, int cubeX, int y) {}

    private HudCoords calculateCoords(GuiGraphicsExtractor graphics, int textWidth, int cubeWidth) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        HudSection.Position position = ConfigManager.get().hud.regionNamePosition;
        int margin = 10;
        int gap = 4;
        int yTop = 10;
        int yBottom = screenH - 10 - minecraft.font.lineHeight;

        return switch (position) {
            case TOP_LEFT -> new HudCoords(margin + cubeWidth + gap, margin, yTop);
            case TOP_RIGHT -> new HudCoords(screenW - textWidth - cubeWidth - margin - gap, screenW - cubeWidth - margin, yTop);
            case BOTTOM_LEFT -> new HudCoords(margin + cubeWidth + gap, margin, yBottom);
            case BOTTOM_RIGHT -> new HudCoords(screenW - textWidth - cubeWidth - margin - gap, screenW - cubeWidth - margin, yBottom);
        };
    }

    public static void register() {
        HudElementRegistry.addLast(PrivitMod.id("region_name_overlay"), INSTANCE);
    }
}