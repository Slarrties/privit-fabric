package dev.slarrties.privit.server.region.protection.handler.interact_with_animals;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.*;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.INTERACT_WITH_ANIMALS)
public final class InteractAnimalsHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.INTERACT_WITH_ANIMALS;
    }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof AnimalEntity)) return ActionResult.PASS;

            ItemStack stack = serverPlayer.getStackInHand(hand);
            if (!isProtectedAnimalInteraction(entity, stack)) return ActionResult.PASS;

            BlockPos pos = entity.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_ANIMALS, pos);

            if (!allowed) {
                denyInteraction(serverPlayer, stack);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
    private boolean isProtectedAnimalInteraction(Entity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            if (entity instanceof AbstractHorseEntity horse && !horse.isTame()) return true;
            if (entity instanceof CamelEntity camel && !camel.isTame()) return true;
            return false;
        }

        if (entity instanceof AnimalEntity animal) {
            if (animal.isBreedingItem(stack)) {
                return true;
            }
        }

        if (entity instanceof TameableEntity tameable && tameable.isBreedingItem(stack)) {
            return true;
        }

        if (entity instanceof WolfEntity && stack.isOf(Items.BONE)) {
            return true;
        }

        if (entity instanceof SheepEntity || entity instanceof MooshroomEntity) {
            if (stack.isOf(Items.SHEARS)) return true;
        }

        if (entity instanceof ArmadilloEntity && stack.isOf(Items.BRUSH)) {
            return true;
        }

        if ((entity instanceof CowEntity || entity instanceof MooshroomEntity || entity instanceof GoatEntity)
                && (stack.isOf(Items.BUCKET) || stack.isOf(Items.BOWL))) {
            return true;
        }

        if (entity instanceof AbstractHorseEntity horse) {
            if (stack.isEmpty()) return horse.isTame();
            if (stack.isOf(Items.SADDLE)) return true;
            if (horse instanceof LlamaEntity && stack.isOf(Items.CHEST)) return true;
        }

        if (entity instanceof ParrotEntity) {
            return stack.isOf(Items.COOKIE) ||
                    stack.isOf(Items.WHEAT_SEEDS) ||
                    stack.isOf(Items.MELON_SEEDS) ||
                    stack.isOf(Items.PUMPKIN_SEEDS);
        }

        if (entity instanceof CatEntity && stack.isOf(Items.COD) || stack.isOf(Items.SALMON)) {
            return true;
        }

        if (entity instanceof RabbitEntity) {
            return stack.isOf(Items.CARROT) || stack.isOf(Items.GOLDEN_CARROT) || stack.isOf(Items.DANDELION);
        }

        if (entity instanceof BeeEntity && (stack.isOf(Items.FLOWERING_AZALEA) || stack.isOf(Items.FLOWERING_AZALEA_LEAVES))) {
            return true;
        }

        if (stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.BUCKET) || stack.isOf(Items.AXOLOTL_BUCKET)) {
            return entity instanceof FishEntity ||
                    entity instanceof AxolotlEntity ||
                    entity instanceof TadpoleEntity;
        }

        return false;
    }

    private void denyInteraction(ServerPlayerEntity serverPlayer, ItemStack stack) {
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ANIMAL_TAME_AND_BREED, Color.RED);
        InventorySyncSystem.syncHandSlotStrong(serverPlayer, serverPlayer.getActiveHand());
    }
}