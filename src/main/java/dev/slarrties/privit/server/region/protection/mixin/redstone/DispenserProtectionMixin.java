package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BoneMealUseContext;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.DispenserEntityAssigner;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.decoration.ArmorStand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule({
        Rule.BUILD,
        Rule.ATTACK_PASSIVE_MOBS,
        Rule.USE_SPAWN_EGGS,
        Rule.USE_FIRE_STARTERS,
        Rule.THROW_SNOWBALLS,
        Rule.THROW_EGGS,
        Rule.THROW_POTIONS,
        Rule.INTERACT_WITH_BOATS,
        Rule.INTERACT_WITH_MINECARTS,
        Rule.THROW_WIND_CHARGES,
        Rule.CAUSE_EXPLOSIONS,
        Rule.USE_FLUIDS,
        Rule.USE_BONE_MEAL
})
@Mixin(DispenserBlock.class)
public abstract class DispenserProtectionMixin {

    @Unique
    private static final ThreadLocal<ItemStack> DISPENSED_STACK = new ThreadLocal<>(); // TODO

    @Inject(
            method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onDispenseHead(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (!(level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser)) return;

        int slot = dispenser.getRandomSlot(level.getRandom());
        if (slot < 0) return;

        ItemStack stack = dispenser.getItem(slot);
        DISPENSED_STACK.set(stack);

        Direction facing = state.getValue(DispenserBlock.FACING);
        BlockPos targetPos = pos.relative(facing);

        if (stack.is(Items.BONE_MEAL)) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(level, pos);
            if (responsible != null) BoneMealUseContext.push(responsible, targetPos);

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_BONE_MEAL, targetPos, level)) {
                sendDenyAndCancel(level, pos, responsible, NotificationType.DENY_USE_BONE_MEAL, ci);
                BoneMealUseContext.pop();
            }
            return;
        }

        Rule rule = getRuleForInstantCancel(stack);
        if (rule != null) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(level, pos);
            if (!RegionPermissionChecker.isAllowed(responsible, rule, targetPos, level)) {
                sendDenyAndCancel(level, pos, responsible, getNotificationType(rule), ci);
            }
            return;
        }

        if (isWaterBottle(stack)) {
            BlockState targetState = level.getBlockState(targetPos);

            if (targetState.is(BlockTags.CONVERTABLE_TO_MUD)) {
                UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(level, pos);
                if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, targetPos, level)) {
                    sendDenyAndCancel(level, pos, responsible, NotificationType.DENY_PLACE_BLOCK, ci);
                    return;
                }
            }
        }

        if (shouldCancelAnimalOrStandInteraction(stack, level, targetPos)) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(level, pos);

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.ATTACK_PASSIVE_MOBS, targetPos, level)) {
                sendDenyAndCancel(level, pos, responsible, NotificationType.DENY_ANIMAL_TAME_AND_BREED, ci);
            }
        }
    }

    @Inject(
            method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL")
    )
    private void onDispenseTail(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        ItemStack stack = DISPENSED_STACK.get();
        DISPENSED_STACK.remove();
        if (stack == null || stack.isEmpty()) return;

        Direction facing = state.getValue(DispenserBlock.FACING);
        BlockPos targetPos = pos.relative(facing);

        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(level, pos);
        if (responsible == null) return;
        if (stack.is(Items.FLINT_AND_STEEL)) {
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.is(Blocks.FIRE) || targetState.is(BlockTags.FIRE) || targetState.is(Blocks.SOUL_FIRE)) {
                FireOriginTracker fireTracker = WorldRegistry.get(level)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(targetPos, responsible);
            }
        }

        if (stack.getItem() instanceof BucketItem) {
            FluidState fluidState = level.getFluidState(targetPos);
            BlockState blockState = level.getBlockState(targetPos);
            boolean hasFluid = !fluidState.isEmpty() || (blockState.getBlock() instanceof BucketPickup && blockState.getFluidState().isSource());
            if (hasFluid) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(level)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.record(targetPos, responsible);
            }
        }

        DispenserEntityAssigner.assignOwner(level, pos, null, stack);
    }

    @Inject(
            method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("RETURN")
    )
    private void popBoneMealContext(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        BoneMealUseContext.pop();
    }

    @Unique
    private void sendDenyAndCancel(ServerLevel level, BlockPos pos, UUID responsible, NotificationType type, CallbackInfo ci) {
        level.levelEvent(1001, pos, 0);
        ServerPlayer serverPlayer = level.getServer().getPlayerList().getPlayer(responsible);
        PlayerNotification.trySend(serverPlayer, type, Color.RED);
        ci.cancel();
    }

    @Unique
    private boolean shouldCancelAnimalOrStandInteraction(ItemStack stack, ServerLevel level, BlockPos targetPos) {
        var entities = level.getEntitiesOfClass(Entity.class, new AABB(targetPos), e -> true);

        for (Entity entity : entities) {
            if (matchesAnimalOrStandInteraction(entity, stack)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean matchesAnimalOrStandInteraction(Entity entity, ItemStack stack) {
        if (entity instanceof Sheep && stack.is(Items.SHEARS)) return true;
        if (entity instanceof Armadillo && stack.is(Items.BRUSH)) return true;
        if (entity instanceof AbstractHorse ||
                entity instanceof Pig ||
                entity instanceof Strider ||
                entity instanceof Camel) {
            if (stack.is(Items.SADDLE)) return true;
            if (stack.is(Items.LEATHER_HORSE_ARMOR) ||
                    stack.is(Items.IRON_HORSE_ARMOR) ||
                    stack.is(Items.GOLDEN_HORSE_ARMOR) ||
                    stack.is(Items.DIAMOND_HORSE_ARMOR)) {
                return true;
            }
        }

        if (entity instanceof Llama &&
                stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CarpetBlock) {
            return true;
        }

        if ((entity instanceof Llama || entity instanceof Donkey || entity instanceof Mule)
                && stack.is(Items.CHEST)) {
            return true;
        }

        if (entity instanceof ArmorStand) {
            if (stack.has(DataComponents.EQUIPPABLE) ||
                    stack.is(Items.ELYTRA) ||
                    stack.is(Items.CARVED_PUMPKIN) ||
                    stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AbstractSkullBlock ||
                    stack.is(Items.SHIELD)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean isWaterBottle(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem)) return false;
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;

        return contents.is(Potions.WATER);
    }

    @Unique
    private static Rule getRuleForInstantCancel(ItemStack stack) {
        if (stack.is(Items.FLINT_AND_STEEL)) return Rule.USE_FIRE_STARTERS;
        if (stack.getItem() instanceof SpawnEggItem) return Rule.USE_SPAWN_EGGS;
        if (stack.getItem() instanceof BoatItem) return Rule.INTERACT_WITH_BOATS;
        if (stack.getItem() instanceof MinecartItem) return Rule.INTERACT_WITH_MINECARTS;
        if (stack.getItem() instanceof BucketItem) return Rule.USE_FLUIDS;
        return null;
    }

    @Unique
    private static NotificationType getNotificationType(Rule rule) {
        return switch (rule) {
            case USE_FIRE_STARTERS -> NotificationType.DENY_USE_FIRE_STARTER;
            case USE_SPAWN_EGGS -> NotificationType.DENY_USE_SPAWN_EGG;
            case INTERACT_WITH_BOATS -> NotificationType.DENY_INTERACT_BOAT;
            case INTERACT_WITH_MINECARTS -> NotificationType.DENY_INTERACT_MINECART;
            case USE_FLUIDS -> NotificationType.DENY_USE_WATER_BUCKET;
            case BUILD -> NotificationType.DENY_PLACE_BLOCK;
            default -> NotificationType.REGION_DENY_CHANGES;
        };
    }
}