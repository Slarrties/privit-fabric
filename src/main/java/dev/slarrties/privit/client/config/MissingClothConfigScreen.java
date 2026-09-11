package dev.slarrties.privit.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

@Environment(EnvType.CLIENT)
public final class MissingClothConfigScreen extends Screen {

    private final Screen parent;

    public MissingClothConfigScreen(Screen parent) {
        super(Text.translatable("privit.config.missing_cloth.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 100, height - 27, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("privit.config.missing_cloth.message"),
                width / 2,
                70,
                0xAAAAAA
        );
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}