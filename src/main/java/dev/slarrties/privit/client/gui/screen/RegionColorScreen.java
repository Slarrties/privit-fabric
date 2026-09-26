package dev.slarrties.privit.client.gui.screen;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.widget.ButtonTextures;
import dev.slarrties.privit.client.gui.widget.GuiButton;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.Optional;

public class RegionColorScreen extends Screen {

    private final RegionGuiController controller;

    private static final int GAP = 6;
    private static final int COLUMNS = 5;
    private static final int BUTTON_SIZE = 20;
    private static final int BG_WIDTH = 210;
    private static final int BG_HEIGHT = 180;

    private static final Identifier BACKGROUND = Identifier.of(PrivitMod.MOD_ID, "textures/gui/region_gui_background.png");
    private static final ButtonTextures BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/button_bg_common_active.png")
    );
    private static final ButtonTextures WIDE_BUTTON_BACKGROUND = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_pressed.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/wide_button_bg_common_active.png")
    );

    public RegionColorScreen(RegionGuiController controller) {
        super(Text.empty());
        this.controller = controller;
    }

    @Override
    protected void init() {
        int bgX = (this.width - BG_WIDTH) / 2;
        int bgY = (this.height - BG_HEIGHT) / 2;
        int gridWidth = COLUMNS * BUTTON_SIZE + (COLUMNS - 1) * GAP;
        int startX = bgX + (BG_WIDTH - gridWidth) / 2;
        int startY = bgY + 30;

        Color currentColor = this.controller.getLocalState().color();
        Color[] colors = Color.values();

        for (int i = 0; i < colors.length; i++) {
            Color color = colors[i];
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            int x = startX + col * (BUTTON_SIZE + GAP);
            int y = startY + row * (BUTTON_SIZE + GAP);

            GuiButton button = new GuiButton.Builder(Text.empty(), btn -> {
                if (color == this.controller.getLocalState().color()) return;

                RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                        this.controller.getLocalState().id(),
                        true,
                        MinecraftClient.getInstance().player.getName().getString(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.of(color), Optional.empty(), Optional.empty()
                );

                this.controller.sendUpdate(packet);
                client.setScreen(new RegionScreen(this.controller));
            })
                    .dimensions(x, y, BUTTON_SIZE, BUTTON_SIZE)
                    .setBackground(BUTTON_BACKGROUND)
                    .icon(getColorIcon(color), BUTTON_SIZE, BUTTON_SIZE, 0, -2)
                    .build();

            if (color == currentColor) button.active = false;

            addDrawableChild(button);
        }

        int cancelWidth = 100;
        int cancelHeight = 20;
        int cancelX = bgX + (BG_WIDTH - cancelWidth) / 2;
        int cancelY = startY + 3 * (BUTTON_SIZE + GAP) + 25;

        addDrawableChild(new GuiButton.Builder(Text.translatable("privit.gui.button.cancel"), btn -> {
            client.setScreen(new RegionScreen(this.controller));
        })
                .dimensions(cancelX, cancelY, cancelWidth, cancelHeight)
                .setBackground(WIDE_BUTTON_BACKGROUND)
                .build());
    }

    private Identifier getColorIcon(Color color) {
        String iconPath = switch (color) {
            case BLACK -> "textures/gui/icon_black.png";
            case DARK_GRAY -> "textures/gui/icon_dark_gray.png";
            case DARK_BLUE -> "textures/gui/icon_dark_blue.png";
            case DARK_GREEN -> "textures/gui/icon_dark_green.png";
            case DARK_AQUA -> "textures/gui/icon_dark_aqua.png";
            case DARK_RED -> "textures/gui/icon_dark_red.png";
            case DARK_PURPLE -> "textures/gui/icon_dark_purple.png";
            case GOLD -> "textures/gui/icon_gold.png";
            case GRAY -> "textures/gui/icon_gray.png";
            case BLUE -> "textures/gui/icon_blue.png";
            case GREEN -> "textures/gui/icon_green.png";
            case AQUA -> "textures/gui/icon_aqua.png";
            case RED -> "textures/gui/icon_red.png";
            case LIGHT_PURPLE -> "textures/gui/icon_purple.png";
            case YELLOW -> "textures/gui/icon_yellow.png";
            case WHITE -> "textures/gui/icon_white.png";
        };
        return Identifier.of(PrivitMod.MOD_ID, iconPath);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int bgX = (this.width - BG_WIDTH) / 2;
        int bgY = (this.height - BG_HEIGHT) / 2;

        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("privit.gui.color.text.title"), bgX + BG_WIDTH / 2, bgY + 15, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context) {
        int x = (this.width - BG_WIDTH) / 2;
        int y = (this.height - BG_HEIGHT) / 2;

        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        context.drawTexture(BACKGROUND, x, y, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    @Override
    public void close() {
        client.setScreen(new RegionScreen(this.controller));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}