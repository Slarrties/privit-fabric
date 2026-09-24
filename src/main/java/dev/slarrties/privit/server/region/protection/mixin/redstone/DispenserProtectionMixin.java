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

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.entity.Entity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.item.BoatItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.potion.Potions;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.tag.BlockTags;
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
        Rule.CAUSE_EXPLOSIONS,
        Rule.USE_FLUIDS,
        Rule.USE_BONE_MEAL
})
@Mixin(DispenserBlock.class)
public abstract class DispenserProtectionMixin {

    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    private void onDispenseHead(ServerWorld serverWorld, BlockPos pos, CallbackInfo ci) {
        if (!(serverWorld.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser)) return;

        int slot = dispenser.chooseNonEmptySlot(serverWorld.random);
        if (slot < 0) return;

        ItemStack stack = dispenser.getStack(slot);
        BlockState state = serverWorld.getBlockState(pos);
        Direction facing = state.get(DispenserBlock.FACING);
        BlockPos targetPos = pos.offset(facing);

        if (stack.isOf(Items.BONE_MEAL)) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);
            if (responsible != null) BoneMealUseContext.push(responsible, targetPos);

            if (!RegionPermissionChecker.isAllowed(responsible, Rule.USE_BONE_MEAL, targetPos, serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_BONE_MEAL, Color.RED);
                serverWorld.syncWorldEvent(1001, pos, 0);
                BoneMealUseContext.pop();
                ci.cancel();
            }
            return;
        }

        Rule rule = getRuleForInstantCancel(stack);
        if (rule != null) {
            UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);
            if (!RegionPermissionChecker.isAllowed(responsible, rule, targetPos, serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
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
                    ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
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
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ANIMAL_TAME_AND_BREED, Color.RED);
                serverWorld.syncWorldEvent(1001, pos, 0);
                ci.cancel();
            }
        }
    }

    @Inject(method = "dispense", at = @At("TAIL"))
    private void onDispenseTail(ServerWorld serverWorld, BlockPos pos, CallbackInfo ci) {
        if (!(serverWorld.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser)) return;

        int slot = dispenser.chooseNonEmptySlot(serverWorld.random);
        if (slot < 0) return;

        ItemStack stack = dispenser.getStack(slot);
        BlockState state = serverWorld.getBlockState(pos);
        Direction facing = state.get(DispenserBlock.FACING);
        BlockPos targetPos = pos.offset(facing);

        UUID responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, pos);
        if (responsible == null) return;

        if (stack.isOf(Items.FLINT_AND_STEEL)) {
            BlockState targetState = serverWorld.getBlockState(targetPos);
            if (targetState.getBlock() instanceof FireBlock ||
                    targetState.isIn(BlockTags.FIRE) ||
                    targetState.isOf(Blocks.SOUL_FIRE)) {
                FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(targetPos, responsible);
            }
        }

        if (stack.getItem() instanceof BucketItem) {
            FluidState fluidState = serverWorld.getFluidState(targetPos);
            BlockState blockState = serverWorld.getBlockState(targetPos);
            boolean hasFluid = !fluidState.isEmpty() || (blockState.getBlock() instanceof Waterloggable && blockState.getFluidState().isStill());

            if (hasFluid) {
                FluidOriginTracker fluidOriginTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFluidOriginTracker();
                fluidOriginTracker.record(targetPos, responsible);
            }
        }

        DispenserEntityAssigner.assignOwner(serverWorld, pos, null, stack);
    }

    @Inject(method = "dispense", at = @At("RETURN"))
    private void popBoneMealContext(ServerWorld world, BlockPos pos, CallbackInfo ci) {
        BoneMealUseContext.pop();
    }

    @Unique
    private boolean shouldCancelAnimalOrStandInteraction(ItemStack stack, ServerWorld world, BlockPos targetPos) {
        var entities = world.getEntitiesByClass(Entity.class, new Box(targetPos), e -> true);

        for (Entity entity : entities) {
            if (matchesAnimalOrStandInteraction(entity, stack))
                return true;
        }
        return false;
    }

    @Unique
    private boolean matchesAnimalOrStandInteraction(Entity entity, ItemStack stack) {
        if (entity instanceof SheepEntity && stack.isOf(Items.SHEARS)) return true;
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

        if (entity instanceof LlamaEntity && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CarpetBlock)
            return true;

        if ((entity instanceof LlamaEntity || entity instanceof DonkeyEntity || entity instanceof MuleEntity) && stack.isOf(Items.CHEST))
            return true;

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
        return PotionUtil.getPotion(stack) == Potions.WATER;
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