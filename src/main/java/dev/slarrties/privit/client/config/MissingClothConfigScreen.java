package dev.slarrties.privit.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;

@Environment(EnvType.CLIENT)
public final class MissingClothConfigScreen extends Screen {

    private final Screen parent;

    public MissingClothConfigScreen(Screen parent) {
        super(Component.translatable("privit.config.missing_cloth.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Button button = Button.builder(
                        Component.translatable("gui.done"),
                        b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build();

        this.addRenderableWidget(button);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 40, 0xFFFFFF);
        graphics.centeredText(
                this.font,
                Component.translatable("privit.config.missing_cloth.message"),
                this.width / 2,
                70,
                0xAAAAAA
        );
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}