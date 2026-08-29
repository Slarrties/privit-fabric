package dev.slarrties.privit.server.region.protection.mixin.redstone;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FluidOriginTracker;
import dev.slarrties.privit.server.tracking.redstone.DispenserEntityAssigner;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.item.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.potion.Potions;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

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
        Rule.USE_FLUIDS
})
@Mixin(DispenserBlock.class)
public abstract class DispenserProtectionMixin {

    @Inject(
            method = "dispense(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onDispenseHead(ServerWorld serverWorld, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (!(serverWorld.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser)) return;

        int slot = dispenser.chooseNonEmptySlot(serverWorld.random);
        if (slot < 0) return;

        ItemStack stack = dispenser.getStack(slot);
        Direction facing = state.get(DispenserBlock.FACING);
        BlockPos targetPos = pos.offset(facing);
        Rule rule = getRuleForInstantCancel(stack);

        if (rule != null) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);

            if (!RegionPermissionChecker.isAllowed(responsible, rule, targetPos, serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer()
                        .getPlayerManager()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, getNotificationType(rule), Color.RED);
                serverWorld.syncWorldEvent(1001, pos, 0);
                ci.cancel();
            }

            return;
        }

        if (isWaterBottle(stack)) {
            BlockState targetState = serverWorld.getBlockState(targetPos);

            if (targetState.isIn(BlockTags.CONVERTABLE_TO_MUD)) {
                UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);

                if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, targetPos, serverWorld)) {
                    ServerPlayerEntity serverPlayer = serverWorld.getServer()
                            .getPlayerManager()
                            .getPlayer(responsible);
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PLACE_BLOCK, Color.RED);
                    serverWorld.syncWorldEvent(1001, pos, 0);
                    ci.cancel();
                    return;
                }
            }
        }

        if (shouldCancelAnimalOrStandInteraction(stack, serverWorld, targetPos)) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_ANIMALS, targetPos, serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer()
                        .getPlayerManager()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ANIMAL_TAME_AND_BREED, Color.RED);
                serverWorld.syncWorldEvent(1001, pos, 0);
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "dispense(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At("TAIL")
    )
    private void onDispenseTail(ServerWorld world, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (!(world.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser)) return;

        int slot = dispenser.chooseNonEmptySlot(world.random);
        if (slot < 0) return;

        ItemStack stack = dispenser.getStack(slot);
        Direction facing = state.get(DispenserBlock.FACING);
        BlockPos targetPos = pos.offset(facing);
        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(world, pos);

        if (responsible == null) return;
        if (stack.isOf(Items.FLINT_AND_STEEL)) {
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.getBlock() instanceof FireBlock ||
                    targetState.isIn(BlockTags.FIRE) ||
                    targetState.isOf(Blocks.SOUL_FIRE)) {
                FireOriginTracker fireTracker = WorldRegistry.get(world)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(targetPos, responsible);
            }
        }

        if (stack.getItem() instanceof BucketItem) {
            FluidState fluidState = world.getFluidState(targetPos);
            BlockState blockState = world.getBlockState(targetPos);
            boolean hasFluid = !fluidState.isEmpty() || (blockState.getBlock() instanceof Waterloggable && blockState.getFluidState().isStill());

            if (hasFluid) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(world)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.record(targetPos, responsible);
            }
        }

        DispenserEntityAssigner.assignOwner(world, pos, null, stack);
    }

    @Unique
    private boolean shouldCancelAnimalOrStandInteraction(ItemStack stack, ServerWorld world, BlockPos targetPos) {
        var entities = world.getEntitiesByClass(Entity.class, new Box(targetPos), e -> true);

        for (Entity entity : entities) {
            if (matchesAnimalOrStandInteraction(entity, stack)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean matchesAnimalOrStandInteraction(Entity entity, ItemStack stack) {
        if (entity instanceof SheepEntity && stack.isOf(Items.SHEARS)) return true;

        if (entity instanceof ArmadilloEntity && stack.isOf(Items.BRUSH)) return true;

        if (entity instanceof AbstractHorseEntity ||
                entity instanceof PigEntity ||
                entity instanceof StriderEntity ||
                entity instanceof CamelEntity) {

            if (stack.isOf(Items.SADDLE)) return true;
            if (stack.isOf(Items.LEATHER_HORSE_ARMOR) ||
                    stack.isOf(Items.IRON_HORSE_ARMOR) ||
                    stack.isOf(Items.GOLDEN_HORSE_ARMOR) ||
                    stack.isOf(Items.DIAMOND_HORSE_ARMOR)) {
                return true;
            }
        }

        if (entity instanceof LlamaEntity &&
                stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CarpetBlock) {
            return true;
        }

        if ((entity instanceof LlamaEntity || entity instanceof DonkeyEntity || entity instanceof MuleEntity)
                && stack.isOf(Items.CHEST)) {
            return true;
        }

        if (entity instanceof ArmorStandEntity) {
            if (stack.getItem() instanceof ArmorItem ||
                    stack.isOf(Items.ELYTRA) ||
                    stack.isOf(Items.CARVED_PUMPKIN) ||
                    stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AbstractSkullBlock ||
                    stack.isOf(Items.SHIELD)) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private static boolean isWaterBottle(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem)) return false;

        var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) return false;

        return contents.potion().get().matchesKey(Potions.WATER.getKey().orElseThrow());
    }

    @Unique
    private static Rule getRuleForInstantCancel(ItemStack stack) {
        if (stack.isOf(Items.FLINT_AND_STEEL)) return Rule.USE_FIRE_STARTERS;
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