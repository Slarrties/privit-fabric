package dev.slarrties.privit.client.gui.widget;

import dev.slarrties.privit.PrivitMod;
import dev.slarrties.privit.client.gui.RegionGuiController;
import dev.slarrties.privit.common.network.payload.c2s.RegionGuiUpdateC2SPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

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
    private final Font font;
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

    private static final Identifier ICON_NORMAL  = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/icon_info.png");
    private static final Identifier ICON_WARNING = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/icon_warning.png");
    private static final Identifier ICON_ERROR   = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/icon_error.png");
    private static final Identifier TEXT_FIELD_NORMAL = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg.png");
    private static final Identifier TEXT_FIELD_FOCUSED = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_active.png");
    private static final Identifier TEXT_FIELD_X = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x.png");
    private static final Identifier TEXT_FIELD_X_FOCUSED = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_active.png");
    private static final Identifier TEXT_FIELD_Y = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y.png");
    private static final Identifier TEXT_FIELD_Y_FOCUSED = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_active.png");
    private static final Identifier TEXT_FIELD_Z = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z.png");
    private static final Identifier TEXT_FIELD_Z_FOCUSED = Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_active.png");

    private static final WidgetSprites BUTTON_INFO = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/info_button_bg.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/info_button_bg.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/info_button_bg.png")
    );

    private static final WidgetSprites BUTTON_LEFT_X = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_left.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_left.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_left.png")
    );

    private static final WidgetSprites BUTTON_LEFT_Y = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_left.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_left.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_left.png")
    );

    private static final WidgetSprites BUTTON_LEFT_Z = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_left.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_left.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_left.png")
    );

    private static final WidgetSprites BUTTON_RIGHT_X = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_right.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_right.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_x_right.png")
    );

    private static final WidgetSprites BUTTON_RIGHT_Y = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_right.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_right.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_y_right.png")
    );

    private static final WidgetSprites BUTTON_RIGHT_Z = new WidgetSprites(
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_right.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_right.png"),
            Identifier.fromNamespaceAndPath(PrivitMod.MOD_ID, "textures/gui/text_field_blueprint_bg_z_right.png")
    );

    public RegionAreaWidget(RegionGuiController controller, Font font, int x, int y, int width, int fieldHeight) {
        this.controller = controller;
        this.font = font;
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

        posYField = new CustomTextField(font,
                centerX - fieldWidth / 2, centerY - verticalGap + 2,
                fieldWidth + 3, fieldHeight, Component.empty()
        );
        posYField.setMaxLength(4);
        posYField.setResponder(this::onFieldChanged);
        posYField.setTooltip(Tooltip.create(Component.literal("+Y")));
        posYField.setCustomBackground(TEXT_FIELD_Y, TEXT_FIELD_Y_FOCUSED);
        posYField.setNumericOnly(true);

        posYPlusButton = new GuiButton.Builder(Component.literal("+"), btn -> adjustField(posYField, +1))
                .dimensions((int) (posYField.getX() + posYField.getWidth() / 1.2), posYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Y)
                .size(buttonSize, buttonSize)
                .build();
        posYMinusButton = new GuiButton.Builder(Component.literal("-"), btn -> adjustField(posYField, -1))
                .dimensions((int) (posYField.getX() - buttonSize / 1.9), posYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Y)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // Y-
        // =====================================================================

        negYField = new CustomTextField(font,
                centerX - fieldWidth / 2, centerY + verticalGap * 2 - 2,
                fieldWidth, fieldHeight, Component.empty()
        );
        negYField.setMaxLength(4);
        negYField.setResponder(this::onFieldChanged);
        negYField.setTooltip(Tooltip.create(Component.literal("-Y")));
        negYField.setCustomBackground(TEXT_FIELD_Y, TEXT_FIELD_Y_FOCUSED);
        negYField.setNumericOnly(true);

        negYPlusButton = new GuiButton.Builder(Component.literal("+"), btn -> adjustField(negYField, +1))
                .dimensions((int) (negYField.getX() + negYField.getWidth() / 1.2), negYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Y)
                .size(buttonSize, buttonSize)
                .build();
        negYMinusButton = new GuiButton.Builder(Component.literal("-"), btn -> adjustField(negYField, -1))
                .dimensions((int) (negYField.getX() - buttonSize / 1.9), negYField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Y)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // X+
        // =====================================================================

        posXField = new CustomTextField(font,
                centerX - offsetX - fieldWidth, centerY,
                fieldWidth, fieldHeight, Component.empty()
        );
        posXField.setMaxLength(4);
        posXField.setResponder(this::onFieldChanged);
        posXField.setTooltip(Tooltip.create(Component.literal("+X")));
        posXField.setCustomBackground(TEXT_FIELD_X, TEXT_FIELD_X_FOCUSED);
        posXField.setNumericOnly(true);

        posXPlusButton = new GuiButton.Builder(Component.literal("+"), btn -> adjustField(posXField, +1))
                .dimensions((int) (posXField.getX() + posXField.getWidth() / 1.2), posXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_X)
                .size(buttonSize, buttonSize)
                .build();
        posXMinusButton = new GuiButton.Builder(Component.literal("-"), btn -> adjustField(posXField, -1))
                .dimensions((int) (posXField.getX() - buttonSize / 1.9), posXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_X)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // X-
        // =====================================================================

        negXField = new CustomTextField(font,
                centerX + offsetX, centerY + verticalGap,
                fieldWidth, fieldHeight, Component.empty()
        );
        negXField.setMaxLength(4);
        negXField.setResponder(this::onFieldChanged);
        negXField.setTooltip(Tooltip.create(Component.literal("-X")));
        negXField.setCustomBackground(TEXT_FIELD_X, TEXT_FIELD_X_FOCUSED);
        negXField.setNumericOnly(true);

        negXPlusButton = new GuiButton.Builder(Component.literal("+"), btn -> adjustField(negXField, +1))
                .dimensions((int) (negXField.getX() + negXField.getWidth() / 1.2), negXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_X)
                .size(buttonSize, buttonSize)
                .build();
        negXMinusButton = new GuiButton.Builder(Component.literal("-"), btn -> adjustField(negXField, -1))
                .dimensions((int) (negXField.getX() - buttonSize / 1.9), negXField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_X)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // Z+
        // =====================================================================

        posZField = new CustomTextField(font,
                centerX - offsetX - fieldWidth, centerY + verticalGap,
                fieldWidth, fieldHeight, Component.empty()
        );
        posZField.setMaxLength(4);
        posZField.setResponder(this::onFieldChanged);
        posZField.setTooltip(Tooltip.create(Component.literal("+Z")));
        posZField.setCustomBackground(TEXT_FIELD_Z, TEXT_FIELD_Z_FOCUSED);
        posZField.setNumericOnly(true);

        posZPlusButton = new GuiButton.Builder(Component.literal("+"), btn -> adjustField(posZField, +1))
                .dimensions((int) (posZField.getX() + posZField.getWidth() / 1.2), posZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Z)
                .size(buttonSize, buttonSize)
                .build();
        posZMinusButton = new GuiButton.Builder(Component.literal("-"), btn -> adjustField(posZField, -1))
                .dimensions((int) (posZField.getX() - buttonSize / 1.9), posZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Z)
                .size(buttonSize, buttonSize)
                .build();

        // =====================================================================
        // Z-
        // =====================================================================

        negZField = new CustomTextField(font,
                centerX + offsetX,
                centerY,
                fieldWidth, fieldHeight, Component.empty()
        );
        negZField.setMaxLength(4);
        negZField.setResponder(this::onFieldChanged);
        negZField.setTooltip(Tooltip.create(Component.literal("-Z")));
        negZField.setCustomBackground(TEXT_FIELD_Z, TEXT_FIELD_Z_FOCUSED);
        negZField.setNumericOnly(true);

        negZPlusButton = new GuiButton.Builder(Component.literal("+"), btn -> adjustField(negZField, +1))
                .dimensions((int) (negZField.getX() + negZField.getWidth() / 1.2), negZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_RIGHT_Z)
                .size(buttonSize, buttonSize)
                .build();
        negZMinusButton = new GuiButton.Builder(Component.literal("-"), btn -> adjustField(negZField, -1))
                .dimensions((int) (negZField.getX() - buttonSize / 1.9), negZField.getY(), buttonSize, buttonSize)
                .setBackground(BUTTON_LEFT_Z)
                .size(buttonSize, buttonSize)
                .build();

        infoButton = new GuiButton.Builder(Component.empty(), btn -> {})
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
        BoundingBox draft = state.draftBounds();
        BoundingBox original = state.realBounds();
        boolean isAreaLimitExceeded = state.isAreaLimitExceeded();
        long draftVolume = volumeOf(draft);
        long originalVolume = original != null ? volumeOf(original) : 0L;
        List<BoundingBox> conflicts = state.conflictBounds();
        boolean hasConflicts = conflicts != null && !conflicts.isEmpty();
        boolean noRealBounds = original == null;

        List<Component> lines = new ArrayList<>();

        if (original != null) {
            MutableComponent originalLine = Component.translatable("privit.gui.properties.area_widget.original_prefix")
                    .withStyle(s -> s.withColor(0xFFFFFF))
                    .append(Component.literal(" "))
                    .append(Component.literal(String.valueOf(originalVolume))
                            .withStyle(s -> s.withColor(0xAAAAAA)));
            lines.add(originalLine);
        }

        MutableComponent draftLine = Component.translatable("privit.gui.properties.area_widget.draft_prefix")
                .withStyle(s -> s.withColor(0xFFFFFF))
                .append(Component.literal(" "))
                .append(Component.literal(String.valueOf(draftVolume))
                        .withStyle(s -> s.withColor(isAreaLimitExceeded ? 0xFF5555 : 0xAAAAAA)));
        lines.add(draftLine);

        if (noRealBounds) {
            lines.add(Component.empty());
            lines.add(Component.translatable("privit.gui.properties.area_widget.no_real_bounds")
                    .withStyle(s -> s.withColor(0xFFAA00)));
        }

        if (isAreaLimitExceeded) {
            lines.add(Component.empty());
            lines.add(Component.translatable("privit.gui.properties.area_widget.area_limit_exceeded")
                    .withStyle(s -> s.withColor(0xFF5555)));
        }

        if (hasConflicts) {
            lines.add(Component.empty());
            lines.add(Component.translatable("privit.gui.properties.area_widget.conflicts")
                    .withStyle(s -> s.withColor(0xFF5555)));
        }

        return Tooltip.create(joinLines(lines));
    }

    private long volumeOf(BoundingBox box) {
        if (box == null) return 0L;

        long w = box.maxX() - box.minX() + 1L;
        long h = box.maxY() - box.minY() + 1L;
        long d = box.maxZ() - box.minZ() + 1L;
        return w * h * d;
    }

    private Component joinLines(List<Component> lines) {
        MutableComponent result = Component.empty();

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) result.append(Component.literal("\n"));
            result.append(lines.get(i));
        }
        return result;
    }

    private void adjustField(CustomTextField field, int delta) {
        if (updatingState) return;

        int current = parseInt(field.getValue(), 0);
        int next = Math.max(0, current + delta);

        updatingState = true;
        try {
            field.setValue(String.valueOf(next));
        } finally {
            updatingState = false;
        }
        onFieldChanged(field.getValue());
    }

    private void onFieldChanged(String ignored) {
        if (updatingState) return;

        int posX = parseInt(posXField.getValue(), 0);
        int negX = parseInt(negXField.getValue(), 0);
        int posY = parseInt(posYField.getValue(), 0);
        int negY = parseInt(negYField.getValue(), 0);
        int posZ = parseInt(posZField.getValue(), 0);
        int negZ = parseInt(negZField.getValue(), 0);

        BlockPos pivot = this.controller.getLocalState().pivotPos();
        BoundingBox newDraftBounds = new BoundingBox(
                pivot.getX() - negX, pivot.getY() - negY, pivot.getZ() - negZ,
                pivot.getX() + posX, pivot.getY() + posY, pivot.getZ() + posZ
        );

        RegionGuiUpdateC2SPacket packet = new RegionGuiUpdateC2SPacket(
                this.controller.getLocalState().id(),
                true,
                Minecraft.getInstance().player.getName().getString(),
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
            BoundingBox bounds = this.controller.getLocalState().draftBounds();

            posXField.setValue(String.valueOf(bounds.maxX() - pivot.getX()));
            negXField.setValue(String.valueOf(pivot.getX() - bounds.minX()));
            posYField.setValue(String.valueOf(bounds.maxY() - pivot.getY()));
            negYField.setValue(String.valueOf(pivot.getY() - bounds.minY()));
            posZField.setValue(String.valueOf(bounds.maxZ() - pivot.getZ()));
            negZField.setValue(String.valueOf(pivot.getZ() - bounds.minZ()));
            infoButton.setTooltip(buildAreaTooltip());
        } finally {
            updatingState = false;
        }
        updateInfoPointAppearance();
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        posXField.extractRenderState(graphics, mouseX, mouseY, delta);
        posXPlusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        posXMinusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        negXField.extractRenderState(graphics, mouseX, mouseY, delta);
        negXPlusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        negXMinusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        posYField.extractRenderState(graphics, mouseX, mouseY, delta);
        posYPlusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        posYMinusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        negYField.extractRenderState(graphics, mouseX, mouseY, delta);
        negYPlusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        negYMinusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        posZField.extractRenderState(graphics, mouseX, mouseY, delta);
        posZPlusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        posZMinusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        negZField.extractRenderState(graphics, mouseX, mouseY, delta);
        negZPlusButton.extractRenderState(graphics, mouseX, mouseY, delta);
        negZMinusButton.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return posXPlusButton.mouseClicked(event, doubleClick)
                || posXMinusButton.mouseClicked(event, doubleClick)
                || negXPlusButton.mouseClicked(event, doubleClick)
                || negXMinusButton.mouseClicked(event, doubleClick)
                || posYPlusButton.mouseClicked(event, doubleClick)
                || posYMinusButton.mouseClicked(event, doubleClick)
                || negYPlusButton.mouseClicked(event, doubleClick)
                || negYMinusButton.mouseClicked(event, doubleClick)
                || posZPlusButton.mouseClicked(event, doubleClick)
                || posZMinusButton.mouseClicked(event, doubleClick)
                || negZPlusButton.mouseClicked(event, doubleClick)
                || negZMinusButton.mouseClicked(event, doubleClick)
                || posXField.mouseClicked(event, doubleClick)
                || negXField.mouseClicked(event, doubleClick)
                || posYField.mouseClicked(event, doubleClick)
                || negYField.mouseClicked(event, doubleClick)
                || posZField.mouseClicked(event, doubleClick)
                || negZField.mouseClicked(event, doubleClick);
    }

    public List<EditBox> getFields() {
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