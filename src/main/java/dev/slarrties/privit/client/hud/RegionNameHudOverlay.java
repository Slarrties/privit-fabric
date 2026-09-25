package dev.slarrties.privit.client.hud;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.config.ConfigManager;
import dev.slarrties.privit.common.config.sections.HudSection;

import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.Objects;

public final class RegionNameHudOverlay implements HudRenderCallback {
    public static final RegionNameHudOverlay INSTANCE = new RegionNameHudOverlay();

    private static final String CUBE_SYMBOL = "\uE000";
    private static final Identifier CUSTOM_FONT = Identifier.of(PrivitMod.MOD_ID, "default");

    private static final long APPEAR_DURATION = 800L;
    private static final long HOLD_DURATION = 4000L;
    private static final long FADE_DURATION = 800L;

    private String regionName = null;
    private Color regionColor = Color.WHITE;
    private long appearStart = 0L;
    private long fadeStart = 0L;

    private MutableText cachedNameText = null;
    private MutableText cachedCubeText = null;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;

        if (regionName == null) {
            int rgb = regionColor != null ? regionColor.getColorValue() : 0xFFFFFF;
            this.cachedNameText = Text.translatable("privit.hud.outside_region")
                    .setStyle(Style.EMPTY.withColor(rgb));
            this.cachedCubeText = Text.literal(CUBE_SYMBOL)
                    .setStyle(Style.EMPTY.withFont(CUSTOM_FONT).withColor(rgb));
        } else {
            int rgb = regionColor.getColorValue();
            Style coloredStyle = Style.EMPTY.withFont(CUSTOM_FONT).withColor(rgb);
            Style nameStyle = Style.EMPTY.withColor(rgb);

            this.cachedNameText = Text.literal(regionName).setStyle(nameStyle);
            this.cachedCubeText = Text.literal(CUBE_SYMBOL).setStyle(coloredStyle);
        }

        this.cachedNameWidth = client.textRenderer.getWidth(cachedNameText);
        this.cachedCubeWidth = client.textRenderer.getWidth(cachedCubeText);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;
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

            renderOutsideText(context, alpha);
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

        HudCoords coords = calculateCoords(client, cachedNameWidth, cachedCubeWidth);

        if (nameAlpha > 0.0f) {
            int nameColor = ((int) (nameAlpha * appearAlpha * 255F) << 24) | 0xFFFFFF;
            context.drawTextWithShadow(client.textRenderer, cachedNameText, coords.textX(), coords.y(), nameColor);
        }

        if (appearAlpha > 0.0f) {
            int cubeColor = ((int) (appearAlpha * 255F) << 24) | 0xFFFFFF;
            context.drawTextWithShadow(client.textRenderer, cachedCubeText, coords.cubeX(), coords.y(), cubeColor);
        }
    }

    private void renderOutsideText(DrawContext context, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        HudCoords coords = calculateCoords(client, cachedNameWidth, cachedCubeWidth);
        int finalColor = ((int) (alpha * 255F) << 24) | 0xFFFFFF;

        context.drawTextWithShadow(client.textRenderer, cachedNameText, coords.textX(), coords.y(), finalColor);
        context.drawTextWithShadow(client.textRenderer, cachedCubeText, coords.cubeX(), coords.y(), finalColor);
    }

    private record HudCoords(int textX, int cubeX, int y) {}

    private HudCoords calculateCoords(MinecraftClient client, int textWidth, int cubeWidth) {
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        HudSection.Position position = ConfigManager.get().hud.regionNamePosition;
        int margin = 10;
        int gap = 4;
        int yTop = 10;
        int yBottom = screenH - 10 - client.textRenderer.fontHeight;

        return switch (position) {
            case TOP_LEFT -> new HudCoords(margin + cubeWidth + gap, margin, yTop);
            case TOP_RIGHT -> new HudCoords(screenW - textWidth - cubeWidth - margin - gap, screenW - cubeWidth - margin, yTop);
            case BOTTOM_LEFT -> new HudCoords(margin + cubeWidth + gap, margin, yBottom);
            case BOTTOM_RIGHT -> new HudCoords(screenW - textWidth - cubeWidth - margin - gap, screenW - cubeWidth - margin, yBottom);
        };
    }

    public static void register() {
        HudRenderCallback.EVENT.register(INSTANCE);
    }
}