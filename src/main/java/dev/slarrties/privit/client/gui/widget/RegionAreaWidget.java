package dev.slarrties.privit.client.gui.widget;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ButtonTextures;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

public class RegionAreaWidget {

    private enum InfoState {
        NORMAL,
        WARNING,
        ERROR
    }

    private final RegionGuiController controller;
    private final TextRenderer textRenderer;
    private final int x, y, width, height;
    private boolean updatingState = false;

    private final CustomTextField posXField;
    private final CustomTextField negXField;
    private final CustomTextField posYField;
    private final CustomTextField negYField;
    private final CustomTextField posZField;
    private final CustomTextField negZField;

    private final GuiButton infoButton;
    private final GuiButton posXPlusButton;
    private final GuiButton negXPlusButton;
    private final GuiButton posYPlusButton;
    private final GuiButton negYPlusButton;
    private final GuiButton posZPlusButton;
    private final GuiButton negZPlusButton;
    private final GuiButton posXMinusButton;
    private final GuiButton negXMinusButton;
    private final GuiButton posYMinusButton;
    private final GuiButton negYMinusButton;
    private final GuiButton posZMinusButton;
    private final GuiButton negZMinusButton;

    private static final Identifier ICON_NORMAL  = Identifier.of(PrivitMod.MOD_ID, "textures/gui/icon_info.png");
    private static final Identifier ICON_WARNING = Identifier.of(PrivitMod.MOD_ID, "textures/gui/icon_warning.png");
    private static final Identifier ICON_ERROR   = Identifier.of(PrivitMod.MOD_ID, "textures/gui/icon_error.png");

    private static final Identifier TEXT_FIELD_NORMAL = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg.png");
    private static final Identifier TEXT_FIELD_FOCUSED = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_active.png");
    private static final Identifier TEXT_FIELD_X = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x.png");
    private static final Identifier TEXT_FIELD_X_FOCUSED = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_active.png");
    private static final Identifier TEXT_FIELD_Y = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y.png");
    private static final Identifier TEXT_FIELD_Y_FOCUSED = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_active.png");
    private static final Identifier TEXT_FIELD_Z = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z.png");
    private static final Identifier TEXT_FIELD_Z_FOCUSED = Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_active.png");

    private static final ButtonTextures BUTTON_INFO = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/info_button_bg.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/info_button_bg.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/info_button_bg.png")
    );

    private static final ButtonTextures BUTTON_LEFT_X = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_left.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_left.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_left.png")
    );

    private static final ButtonTextures BUTTON_LEFT_Y = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_left.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_left.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_left.png")
    );

    private static final ButtonTextures BUTTON_LEFT_Z = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_left.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_left.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_left.png")
    );

    private static final ButtonTextures BUTTON_RIGHT_X = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_right.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_right.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_right.png")
    );

    private static final ButtonTextures BUTTON_RIGHT_Y = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_right.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_right.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_right.png")
    );

    private static final ButtonTextures BUTTON_RIGHT_Z = new ButtonTextures(
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_right.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_right.png"),
            Identifier.of(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_right.png")
    );


    public RegionAreaWidget(RegionGuiController controller, TextRenderer textRenderer, int x, int y, int width, int fieldHeight) {
        this.controller = controller;
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = fieldHeight * 3 + 20;

        int fieldWidth = 32;
        int centerX = x + width / 2;
        int centerY = y + this.height / 2 - fieldHeight - 7;
        int offsetX = 30;
        int verticalGap = fieldHeight + 5;
        int buttonSize = fieldHeight;

        // =====================================================================
        // Y+
        // =====================================================================

        posYField = new CustomTextField(textRenderer,
            centerX - fieldWidth / 2, centerY - verticalGap + 2,
            fieldWidth + 3, fieldHeight, Text.empty()
        );
        posYField.setMaxLength(4);
        posYField.setChangedListener(this::onFieldChanged);
        posYField.setTooltip(Tooltip.of(Text.literal("+Y")));
        posYField.setCustomBackground(TEXT_FIELD_Y, TEXT_FIELD_Y_FOCUSED);
        posYField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d+"));

        posYPlusButton = new GuiButton.Builder(Text.literal("+"), btn -> adjustField(posYField, +1))
                .dimensions((int) (posYField.getX() + posYField.getWidth() / 1.2), posYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Y)
                .size(buttonSize, buttonSize)
                .build();

        posYMinusButton = new GuiButton.Builder(Text.literal("-"), btn -> adjustField(posYField, -1))
                .dimensions((int) (posYField.getX() - buttonSize / 1.9), posYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Y)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // Y-
        // =====================================================================

        negYField = new CustomTextField(textRenderer,
                centerX - fieldWidth / 2, centerY + verticalGap * 2 - 2,
                fieldWidth, fieldHeight, Text.empty()
        );
        negYField.setMaxLength(4);
        negYField.setChangedListener(this::onFieldChanged);
        negYField.setTooltip(Tooltip.of(Text.literal("-Y")));
        negYField.setCustomBackground(TEXT_FIELD_Y, TEXT_FIELD_Y_FOCUSED);
        negYField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d+"));

        negYPlusButton = new GuiButton.Builder(Text.literal("+"), btn -> adjustField(negYField, +1))
                .dimensions((int) (negYField.getX() + negYField.getWidth() / 1.2), negYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Y)
                .size(buttonSize, buttonSize)
                .build();

        negYMinusButton = new GuiButton.Builder(Text.literal("-"), btn -> adjustField(negYField, -1))
                .dimensions((int) (negYField.getX() - buttonSize / 1.9), negYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Y)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // X+
        // =====================================================================

        posXField = new CustomTextField(textRenderer,
                centerX - offsetX - fieldWidth, centerY,
                fieldWidth, fieldHeight, Text.empty()
        );
        posXField.setMaxLength(4);
        posXField.setChangedListener(this::onFieldChanged);
        posXField.setTooltip(Tooltip.of(Text.literal("+X")));
        posXField.setCustomBackground(TEXT_FIELD_X, TEXT_FIELD_X_FOCUSED);
        posXField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d+"));

        posXPlusButton = new GuiButton.Builder(Text.literal("+"), btn -> adjustField(posXField, +1))
                .dimensions((int) (posXField.getX() + posXField.getWidth() / 1.2), posXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_X)
                .size(buttonSize, buttonSize)
                .build();

        posXMinusButton = new GuiButton.Builder(Text.literal("-"), btn -> adjustField(posXField, -1))
                .dimensions((int) (posXField.getX() - buttonSize / 1.9), posXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_X)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // X-
        // =====================================================================

        negXField = new CustomTextField(textRenderer,
                centerX + offsetX, centerY + verticalGap,
                fieldWidth, fieldHeight, Text.empty()
        );
        negXField.setMaxLength(4);
        negXField.setChangedListener(this::onFieldChanged);
        negXField.setTooltip(Tooltip.of(Text.literal("-X")));
        negXField.setCustomBackground(TEXT_FIELD_X, TEXT_FIELD_X_FOCUSED);
        negXField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d+"));

        negXPlusButton = new GuiButton.Builder(Text.literal("+"), btn -> adjustField(negXField, +1))
                .dimensions((int) (negXField.getX() + negXField.getWidth() / 1.2), negXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_X)
                .size(buttonSize, buttonSize)
                .build();

        negXMinusButton = new GuiButton.Builder(Text.literal("-"), btn -> adjustField(negXField, -1))
                .dimensions((int) (negXField.getX() - buttonSize / 1.9), negXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_X)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // Z+
        // =====================================================================

        posZField = new CustomTextField(textRenderer,
                centerX - offsetX - fieldWidth, centerY + verticalGap,
                fieldWidth, fieldHeight, Text.empty()
        );
        posZField.setMaxLength(4);
        posZField.setChangedListener(this::onFieldChanged);
        posZField.setTooltip(Tooltip.of(Text.literal("+Z")));
        posZField.setCustomBackground(TEXT_FIELD_Z, TEXT_FIELD_Z_FOCUSED);
        posZField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d+"));

        posZPlusButton = new GuiButton.Builder(Text.literal("+"), btn -> adjustField(posZField, +1))
                .dimensions((int) (posZField.getX() + posZField.getWidth() / 1.2), posZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Z)
                .size(buttonSize, buttonSize)
                .build();

        posZMinusButton = new GuiButton.Builder(Text.literal("-"), btn -> adjustField(posZField, -1))
                .dimensions((int) (posZField.getX() - buttonSize / 1.9), posZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Z)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // Z-
        // =====================================================================

        negZField = new CustomTextField(textRenderer,
                centerX + offsetX,
                centerY,
                fieldWidth, fieldHeight, Text.empty()
        );
        negZField.setMaxLength(4);
        negZField.setChangedListener(this::onFieldChanged);
        negZField.setTooltip(Tooltip.of(Text.literal("-Z")));
        negZField.setCustomBackground(TEXT_FIELD_Z, TEXT_FIELD_Z_FOCUSED);
        negZField.setTextPredicate(text -> text.isEmpty() || text.matches("\\d+"));

        negZPlusButton = new GuiButton.Builder(Text.literal("+"), btn -> adjustField(negZField, +1))
                .dimensions((int) (negZField.getX() + negZField.getWidth() / 1.2), negZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Z)
                .size(buttonSize, buttonSize)
                .build();

        negZMinusButton = new GuiButton.Builder(Text.literal("-"), btn -> adjustField(negZField, -1))
                .dimensions((int) (negZField.getX() - buttonSize / 1.9), negZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Z)
                .size(buttonSize, buttonSize)
                .build();

        infoButton = new GuiButton.Builder(Text.empty(), btn -> {})
                .dimensions(
                        centerX - buttonSize / 2,
                        centerY + fieldHeight / 2 - buttonSize / 2 + 12,
                        buttonSize,
                        buttonSize
                )
                .setBackground(BUTTON_INFO)
                .build();
        infoButton.setTooltip(buildAreaTooltip());
        infoButton.active = false;

        updateState();
    }

    private Tooltip buildAreaTooltip() {
        var state = controller.getLocalState();
        BlockBox draft = state.draftBounds();
        BlockBox original = state.realBounds();
        boolean isAreaLimitExceeded = state.isAreaLimitExceeded();
        long draftVolume = volumeOf(draft);
        long originalVolume = original != null ? volumeOf(original) : 0L;
        List<BlockBox> conflicts = state.conflictBounds();
        boolean hasConflicts = conflicts != null && !conflicts.isEmpty();
        boolean noRealBounds = original == null;

        List<Text> lines = new ArrayList<>();

        if (original != null) {
            MutableText originalLine = Text.translatable("privit.gui.properties.area_widget.original_prefix")
                    .styled(s -> s.withColor(0xFFFFFF))
                    .append(Text.literal(" "))
                    .append(Text.literal(String.valueOf(originalVolume))
                            .styled(s -> s.withColor(0xAAAAAA)));
            lines.add(originalLine);
        }

        MutableText draftLine = Text.translatable("privit.gui.properties.area_widget.draft_prefix")
                .styled(s -> s.withColor(0xFFFFFF))
                .append(Text.literal(" "))
                .append(Text.literal(String.valueOf(draftVolume))
                        .styled(s -> s.withColor(isAreaLimitExceeded ? 0xFF5555 : 0xAAAAAA)));
        lines.add(draftLine);

        if (noRealBounds) {
            lines.add(Text.empty());
            lines.add(Text.translatable("privit.gui.properties.area_widget.no_real_bounds")
                    .styled(s -> s.withColor(0xFFAA00)));
        }

        if (isAreaLimitExceeded) {
            lines.add(Text.empty());
            lines.add(Text.translatable("privit.gui.properties.area_widget.area_limit_exceeded")
                    .styled(s -> s.withColor(0xFF5555)));
        }

        if (hasConflicts) {
            lines.add(Text.empty());
            lines.add(Text.translatable("privit.gui.properties.area_widget.conflicts")
                    .styled(s -> s.withColor(0xFF5555)));
        }

        return Tooltip.of(joinLines(lines));
    }

    private long volumeOf(BlockBox box) {
        if (box == null) return 0L;
        long w = box.getMaxX() - box.getMinX() + 1L;
        long h = box.getMaxY() - box.getMinY() + 1L;
        long d = box.getMaxZ() - box.getMinZ() + 1L;

        return w * h * d;
    }

    private Text joinLines(List<Text> lines) {
        MutableText result = Text.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) result.append(Text.literal("\n"));
            result.append(lines.get(i));
        }
        return result;
    }

    private void adjustField(CustomTextField field, int delta) {
        if (updatingState) return;

        int current = parseInt(field.getText(), 0);
        int next = Math.max(0, current + delta);

        updatingState = true;
        try {
            field.setText(String.valueOf(next));
        } finally {
            updatingState = false;
        }

        onFieldChanged(field.getText());
    }

    private void onFieldChanged(String ignored) {
        if (updatingState) return;

        int posX = parseInt(posXField.getText(), 0);
        int negX = parseInt(negXField.getText(), 0);
        int posY = parseInt(posYField.getText(), 0);
        int negY = parseInt(negYField.getText(), 0);
        int posZ = parseInt(posZField.getText(), 0);
        int negZ = parseInt(negZField.getText(), 0);

        BlockPos pivot = this.controller.getLocalState().pivotPos();
        BlockBox newDraftBounds = BlockBox.create(
                pivot.add(-negX, -negY, -negZ),
                pivot.add(posX, posY, posZ)
        );

        RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                this.controller.getLocalState().id(),
                true,
                MinecraftClient.getInstance().player.getName().getString(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(newDraftBounds),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        updateInfoPointAppearance();
        this.controller.sendUpdate(packet);
    }

    private int parseInt(String text, int defaultValue) {
        try {
            return text.isEmpty() ? defaultValue : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private InfoState resolveInfoState() {
        var state = controller.getLocalState();
        boolean hasConflicts = state.conflictBounds() != null && !state.conflictBounds().isEmpty();
        boolean areaExceeded = state.isAreaLimitExceeded();
        boolean noRealBounds = state.realBounds() == null;

        if (hasConflicts || areaExceeded) return InfoState.ERROR;
        if (noRealBounds) return InfoState.WARNING;
        return InfoState.NORMAL;
    }

    private void updateInfoPointAppearance() {
        InfoState infoState = resolveInfoState();

        Identifier icon = switch (infoState) {
            case NORMAL  -> ICON_NORMAL;
            case WARNING -> ICON_WARNING;
            case ERROR   -> ICON_ERROR;
        };

        infoButton.setIcon(icon, 18, 18, 0 ,-1);
        infoButton.setTooltip(buildAreaTooltip());
    }

    public void updateState() {
        updatingState = true;

        try {
            BlockPos pivot = this.controller.getLocalState().pivotPos();
            BlockBox bounds = this.controller.getLocalState().draftBounds();

            posXField.setText(String.valueOf(bounds.getMaxX() - pivot.getX()));
            negXField.setText(String.valueOf(pivot.getX() - bounds.getMinX()));
            posYField.setText(String.valueOf(bounds.getMaxY() - pivot.getY()));
            negYField.setText(String.valueOf(pivot.getY() - bounds.getMinY()));
            posZField.setText(String.valueOf(bounds.getMaxZ() - pivot.getZ()));
            negZField.setText(String.valueOf(pivot.getZ() - bounds.getMinZ()));
            infoButton.setTooltip(buildAreaTooltip());
        } finally {
            updatingState = false;
        }

        updateInfoPointAppearance();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        posXField.render(context, mouseX, mouseY, delta);
        posXPlusButton.render(context, mouseX, mouseY, delta);
        posXMinusButton.render(context, mouseX, mouseY, delta);

        negXField.render(context, mouseX, mouseY, delta);
        negXPlusButton.render(context, mouseX, mouseY, delta);
        negXMinusButton.render(context, mouseX, mouseY, delta);

        posYField.render(context, mouseX, mouseY, delta);
        posYPlusButton.render(context, mouseX, mouseY, delta);
        posYMinusButton.render(context, mouseX, mouseY, delta);

        negYField.render(context, mouseX, mouseY, delta);
        negYPlusButton.render(context, mouseX, mouseY, delta);
        negYMinusButton.render(context, mouseX, mouseY, delta);

        posZField.render(context, mouseX, mouseY, delta);
        posZPlusButton.render(context, mouseX, mouseY, delta);
        posZMinusButton.render(context, mouseX, mouseY, delta);

        negZField.render(context, mouseX, mouseY, delta);
        negZPlusButton.render(context, mouseX, mouseY, delta);
        negZMinusButton.render(context, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return posXPlusButton.mouseClicked(mouseX, mouseY, button)
                || posXMinusButton.mouseClicked(mouseX, mouseY, button)
                || negXPlusButton.mouseClicked(mouseX, mouseY, button)
                || negXMinusButton.mouseClicked(mouseX, mouseY, button)
                || posYPlusButton.mouseClicked(mouseX, mouseY, button)
                || posYMinusButton.mouseClicked(mouseX, mouseY, button)
                || negYPlusButton.mouseClicked(mouseX, mouseY, button)
                || negYMinusButton.mouseClicked(mouseX, mouseY, button)
                || posZPlusButton.mouseClicked(mouseX, mouseY, button)
                || posZMinusButton.mouseClicked(mouseX, mouseY, button)
                || negZPlusButton.mouseClicked(mouseX, mouseY, button)
                || negZMinusButton.mouseClicked(mouseX, mouseY, button)
                || posXField.mouseClicked(mouseX, mouseY, button)
                || negXField.mouseClicked(mouseX, mouseY, button)
                || posYField.mouseClicked(mouseX, mouseY, button)
                || negYField.mouseClicked(mouseX, mouseY, button)
                || posZField.mouseClicked(mouseX, mouseY, button)
                || negZField.mouseClicked(mouseX, mouseY, button);
    }

    public List<TextFieldWidget> getFields() {
        return List.of(posXField, negXField, posYField, negYField, posZField, negZField);
    }

    public List<GuiButton> getButtons() {
        return List.of(
                posXPlusButton, posXMinusButton,
                negXPlusButton, negXMinusButton,
                posYPlusButton, posYMinusButton,
                negYPlusButton, negYMinusButton,
                posZPlusButton, posZMinusButton,
                negZPlusButton, negZMinusButton,
                infoButton
        );
    }
}