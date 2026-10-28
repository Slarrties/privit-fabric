package dev.slarrties.privit.server.region.protection.handler.interact_with_animals;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

@AssociatedRule(Rule.INTERACT_WITH_ANIMALS)
public final class InteractAnimalsHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.INTERACT_WITH_ANIMALS;
    }

    @Override
    public void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(entity instanceof Animal)) return InteractionResult.PASS;

            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (!isProtectedAnimalInteraction(entity, stack)) return InteractionResult.PASS;

            BlockPos pos = entity.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_ANIMALS, pos);
            if (!allowed) {
                denyInteraction(serverPlayer, stack);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private boolean isProtectedAnimalInteraction(Entity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            if (entity instanceof AbstractHorse horse && !horse.isTamed()) return true;
            if (entity instanceof Camel camel && !camel.isTamed()) return true;
            return false;
        }
        if (entity instanceof Animal animal) {
            if (animal.isFood(stack)) return true;
        }
        if (entity instanceof TamableAnimal tameable && tameable.isFood(stack)) return true;
        if (entity instanceof Wolf && stack.is(Items.BONE)) return true;
        if (entity instanceof Sheep || entity instanceof MushroomCow) {
            if (stack.is(Items.SHEARS)) return true;
        }
        if (entity instanceof Armadillo && stack.is(Items.BRUSH)) return true;
        if ((entity instanceof Cow || entity instanceof MushroomCow || entity instanceof Goat) && (stack.is(Items.BUCKET) || stack.is(Items.BOWL))) return true;
        if (entity instanceof AbstractHorse horse) {
            if (stack.isEmpty()) return horse.isTamed();
            if (stack.is(Items.SADDLE)) return true;
            if (horse instanceof Llama && stack.is(Items.CHEST)) return true;
        }
        if (entity instanceof Parrot) {
            return stack.is(Items.COOKIE) ||
                    stack.is(Items.WHEAT_SEEDS) ||
                    stack.is(Items.MELON_SEEDS) ||
                    stack.is(Items.PUMPKIN_SEEDS);
        }
        if (entity instanceof Cat && (stack.is(Items.COD) || stack.is(Items.SALMON))) return true;
        if (entity instanceof Rabbit) {
            return stack.is(Items.CARROT) || stack.is(Items.GOLDEN_CARROT) || stack.is(Items.DANDELION);
        }
        if (entity instanceof Bee && (stack.is(Items.FLOWERING_AZALEA) || stack.is(Items.FLOWERING_AZALEA_LEAVES))) return true;
        if (stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET) || stack.is(Items.AXOLOTL_BUCKET)) {
            return entity instanceof AbstractFish ||
                    entity instanceof Axolotl ||
                    entity instanceof Tadpole;
        }
        return false;
    }

    private void denyInteraction(ServerPlayer serverPlayer, ItemStack stack) {
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ANIMAL_TAME_AND_BREED, Color.RED);
        InventorySyncSystem.syncHandSlotStrong(serverPlayer, serverPlayer.getUsedItemHand());
    }
}