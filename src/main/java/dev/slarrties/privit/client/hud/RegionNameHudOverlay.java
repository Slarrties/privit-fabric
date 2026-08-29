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
import net.minecraft.client.render.RenderTickCounter;
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

    private RegionNameHudOverlay() {}

    public void update(String newName, Color newColor) {
        if (newName != null && newName.isBlank()) {
            newName = null;
        }

        boolean changed = !Objects.equals(newName, regionName) || newColor != regionColor;

        if (changed) {
            this.regionName = newName;
            if (this.regionName != null) {
                this.regionColor = newColor;
            }

            long now = System.currentTimeMillis();
            this.appearStart = now;
            this.fadeStart = newName != null ? now + HOLD_DURATION : 0;
        }
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;

        long now = System.currentTimeMillis();

        if (regionName == null) {
            long elapsed = now - appearStart;
            if (elapsed >= FADE_DURATION) return;

            float alpha = 1.0f - Math.min(elapsed, FADE_DURATION) / (float) FADE_DURATION;
            if (alpha < 0.01f) alpha = 0.0f;

            renderOutsideText(context, alpha, this.regionColor);
            return;
        }

        long appearElapsed = now - appearStart;
        long fadeElapsed = now - fadeStart;

        float appearAlpha = appearElapsed < APPEAR_DURATION
                ? Math.min(appearElapsed / (float) APPEAR_DURATION, 1.0f)
                : 1.0f;

        float nameAlpha = fadeElapsed > 0
                ? Math.max(1.0f - fadeElapsed / (float) FADE_DURATION, 0.0f)
                : 1.0f;

        if (nameAlpha < 0.07f) {
            nameAlpha = 0.0f;
        }

        float cubeAlpha = appearAlpha;

        Style coloredStyle = Style.EMPTY
                .withFont(CUSTOM_FONT)
                .withColor(regionColor.getFormatting());

        Style nameStyle = Style.EMPTY.withColor(regionColor.getFormatting());

        MutableText nameText = Text.literal(regionName).setStyle(nameStyle);
        MutableText cubeText = Text.literal(CUBE_SYMBOL).setStyle(coloredStyle);

        int nameWidth = client.textRenderer.getWidth(nameText);
        int cubeWidth = client.textRenderer.getWidth(cubeText);

        HudCoords coords = calculateCoords(client, nameWidth, cubeWidth);

        if (nameAlpha > 0.0f) {
            int nameColor = ((int) (nameAlpha * appearAlpha * 255F) << 24) | 0xFFFFFF;
            context.drawTextWithShadow(client.textRenderer, nameText, coords.textX(), coords.y(), nameColor);
        }

        if (cubeAlpha > 0.0f) {
            int cubeColor = ((int) (cubeAlpha * 255F) << 24) | 0xFFFFFF;
            context.drawTextWithShadow(client.textRenderer, cubeText, coords.cubeX(), coords.y(), cubeColor);
        }
    }

    private void renderOutsideText(DrawContext context, float alpha, Color color) {
        MinecraftClient client = MinecraftClient.getInstance();

        MutableText outside = Text.translatable("privit.hud.outside_region")
                .setStyle(Style.EMPTY.withColor(color.getFormatting()));

        MutableText cube = Text.literal(CUBE_SYMBOL)
                .setStyle(Style.EMPTY.withFont(CUSTOM_FONT).withColor(color.getFormatting()));

        int outsideWidth = client.textRenderer.getWidth(outside);
        int cubeWidth = client.textRenderer.getWidth(cube);

        HudCoords coords = calculateCoords(client, outsideWidth, cubeWidth);

        int baseColor = color.getFormatting().getColorValue() != null
                ? color.getFormatting().getColorValue()
                : 0xFFFFFF;
        int finalColor = ((int) (alpha * 255F) << 24) | (baseColor & 0xFFFFFF);

        context.drawTextWithShadow(client.textRenderer, outside, coords.textX(), coords.y(), finalColor);
        context.drawTextWithShadow(client.textRenderer, cube, coords.cubeX(), coords.y(), finalColor);
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
            case TOP_LEFT -> new HudCoords(
                    margin + cubeWidth + gap,
                    margin,
                    yTop
            );
            case TOP_RIGHT -> new HudCoords(
                    screenW - textWidth - cubeWidth - margin - gap,
                    screenW - cubeWidth - margin,
                    yTop
            );
            case BOTTOM_LEFT -> new HudCoords(
                    margin + cubeWidth + gap,
                    margin,
                    yBottom
            );
            case BOTTOM_RIGHT -> new HudCoords(
                    screenW - textWidth - cubeWidth - margin - gap,
                    screenW - cubeWidth - margin,
                    yBottom
            );
        };
    }

    public static void register() {
        HudRenderCallback.EVENT.register(INSTANCE);
    }
}