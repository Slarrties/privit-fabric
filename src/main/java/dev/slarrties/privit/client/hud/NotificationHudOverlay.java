package dev.slarrties.privit.client.hud;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.notification.NotificationType;

import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.mojang.blaze3d.systems.RenderSystem;

public class NotificationHudOverlay implements HudRenderCallback {

    private static String currentMessage = null;
    private static Color currentColor = Color.WHITE;
    private static int displayTicks = 0;

    private static final int TOTAL_DURATION = 180;
    private static final int FADE_IN_DURATION = 10;
    private static final int FADE_OUT_DURATION = 15;

    public static void showNotification(NotificationType type, Color color) {
        String translationKey = switch (type) {
            case REGION_DENY_CHANGES -> "privit.notification.deny_changes";
            case REGION_CREATED  -> "privit.notification.region_created";
            case REGION_CREATION_FAILED  -> "privit.notification.region_creation_failed";
            case REGION_UPDATED  -> "privit.notification.region_updated";
            case REGION_UPDATE_FAILED  -> "privit.notification.region_update_failed";
            case REGION_DELETED  -> "privit.notification.region_deleted";
            case REGION_DELETION_FAILED  -> "privit.notification.region_deletion_failed";
            case REGION_NOT_FOUND  -> "privit.notification.region_not_found";
            case REGION_TERRITORY_CONFLICT -> "privit.notification.region_territory_conflict";
            case REGION_TOO_BIG -> "privit.notification.region_too_big";
            case REGION_ID_CONFLICT -> "privit.notification.region_id_conflict";
            case REGION_NOT_ACCEPTED -> "privit.notification.region_not_accepted";

            case DENY_MANAGE -> "privit.notification.deny_manage";
            case DENY_BREAK_BLOCK  -> "privit.notification.deny_break_block";
            case DENY_PLACE_BLOCK  -> "privit.notification.deny_place_block";
            case DENY_INTERACT_DOOR -> "privit.notification.deny_interact_door";
            case DENY_INTERACT_TRAPDOOR -> "privit.notification.deny_interact_trapdoor";
            case DENY_INTERACT_LEVER -> "privit.notification.deny_interact_lever";
            case DENY_PRESS_BUTTON -> "privit.notification.deny_press_button";
            case DENY_ITEM_DROP -> "privit.notification.deny_item_drop";
            case DENY_ITEM_PICKUP -> "privit.notification.deny_item_pickup";
            case DENY_TRADE_VILLAGER -> "privit.notification.deny_trade_villager";
            case DENY_ATTACK_PASSIVE_MOB -> "privit.notification.deny_attack_passive_mob";
            case DENY_INTERACT_BOAT -> "privit.notification.deny_interact_boat";
            case DENY_INTERACT_MINECART -> "privit.notification.deny_interact_minecart";
            case DENY_USE_LEASH -> "privit.notification.deny_use_leash";
            case DENY_ANIMAL_TAME_AND_BREED -> "privit.notification.deny_animal_tame_and_breed";
            case DENY_THROW_POTION -> "privit.notification.deny_throw_potion";
            case DENY_USE_FISHING_ROD -> "privit.notification.deny_use_fishing_rod";
            case DENY_USE_DISPENSER -> "privit.notification.deny_use_dispenser";
            case DENY_THROW_ENDER_PEARL -> "privit.notification.deny_throw_ender_pearl";
            case DENY_EAT_CHORUS_FRUIT -> "privit.notification.deny_eat_chorus_fruit";
            case DENY_INTERACT_FENCE_GATE -> "privit.notification.deny_interact_fence_gate";
            case DENY_USE_FIRE_STARTER -> "privit.notification.deny_use_fire_starter";
            case DENY_EXTINGUISH_FIRE -> "privit.notification.deny_extinguish_fire";
            case DENY_THROW_WIND_CHARGE -> "privit.notification.deny_throw_wind_charge";
            case DENY_USE_FROST_WALKER -> "privit.notification.deny_use_frost_walker";
            case DENY_PUSH_ENTITY -> "privit.notification.deny_push_entity";
            case DENY_THROW_SNOWBALL -> "privit.notification.deny_throw_snowball";
            case DENY_THROW_EGG -> "privit.notification.deny_throw_egg";
            case DENY_USE_SPAWN_EGG -> "privit.notification.deny_use_spawn_egg";
            case DENY_CAUSE_EXPLOSION -> "privit.notification.deny_cause_explosion";
            case DENY_USE_WATER_BUCKET -> "privit.notification.deny_use_water_bucket";
            case DENY_USE_LAVA_BUCKET -> "privit.notification.deny_use_lava_bucket";
            case DENY_USE_SPONGE -> "privit.notification.deny_use_sponge";
            case DENY_INTERACT_SIGN -> "privit.notification.deny_interact_sign";
            case DENY_CREATE_NETHER_PORTAL -> "privit.notification.deny_create_nether_portal";
            case DENY_INTERACT_CONTAINER -> "privit.notification.deny_interact_container";
            case DENY_SET_RESPAWN_POINT -> "privit.notification.deny_set_respawn_point";
            case DENY_SPREAD_LIQUID -> "privit.notification.deny_spread_liquid";
            case DENY_PVP -> "privit.notification.deny_pvp";
            case DENY_USE_PISTON -> "privit.notification.deny_use_piston";
            case DENY_USE_TRIAL_MECHANICS -> "privit.notification.deny_use_trial_mechanics";
            case DENY_SPREAD_SCULK -> "privit.notification.deny_spread_sculk";
            case REGIONS_LIMIT_REACHED -> "privit.notification.regions_limit_reached";
        };

        currentMessage = Text.translatable(translationKey).getString();
        currentColor = color != null ? color : Color.WHITE;
        displayTicks = TOTAL_DURATION;
    }

    public static void showNotification(NotificationType type) {
        showNotification(type, Color.WHITE);
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (displayTicks <= 0 || currentMessage == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int textWidth = client.textRenderer.getWidth(currentMessage);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 70;
        float alpha;

        if (displayTicks > TOTAL_DURATION - FADE_IN_DURATION) {
            int fadeTicks = TOTAL_DURATION - displayTicks;
            alpha = (float) fadeTicks / FADE_IN_DURATION;
        } else if (displayTicks <= FADE_OUT_DURATION) {
            alpha = (float) displayTicks / FADE_OUT_DURATION;
        } else {
            alpha = 1.0f;
        }

        int baseColor = currentColor.getFormatting().getColorValue();
        if (baseColor == -1) baseColor = 0xFFFFFF;
        int textColor = (int) (alpha * 255) << 24 | (baseColor & 0xFFFFFF);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.drawTextWithShadow(
                client.textRenderer,
                currentMessage,
                x,
                y,
                textColor
        );

        RenderSystem.disableBlend();
        displayTicks--;

        if (displayTicks <= 0) {
            currentMessage = null;
            currentColor = Color.WHITE;
        }
    }
}