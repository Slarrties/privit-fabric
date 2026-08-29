package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.vehicle.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartProtectionMixin {

    @Inject(method = "moveOnRail", at = @At("HEAD"))
    private void checkMinecartOnRail(BlockPos pos, BlockState state, CallbackInfo ci) {
        checkAndRemoveIfNotAllowed();
    }

    @Inject(method = "moveOffRail", at = @At("HEAD"))
    private void checkMinecartOffRail(CallbackInfo ci) {
        checkAndRemoveIfNotAllowed();
    }

    @Unique
    private void checkAndRemoveIfNotAllowed() {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;

        if(minecart.getWorld() instanceof ServerWorld serverWorld) {
            MinecartOriginTracker minecartOriginTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getMinecartOriginTracker();
            UUID responsible = minecartOriginTracker.getResponsiblePlayer(minecart);
            if (responsible == null) return;

            BlockPos currentPos = minecart.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, currentPos, serverWorld);

            if (!allowed) {
                ItemStack stack = getMinecartItemStack(minecart);

                minecart.dropStack(stack);
                minecart.discard();
                minecartOriginTracker.remove(minecart);

                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            }
        }
    }

    @Unique
    private ItemStack getMinecartItemStack(AbstractMinecartEntity minecart) {
        if (minecart instanceof HopperMinecartEntity) return new ItemStack(Items.HOPPER_MINECART);
        if (minecart instanceof ChestMinecartEntity) return new ItemStack(Items.CHEST_MINECART);
        if (minecart instanceof FurnaceMinecartEntity) return new ItemStack(Items.FURNACE_MINECART);
        if (minecart instanceof TntMinecartEntity) return new ItemStack(Items.TNT_MINECART);
        if (minecart instanceof CommandBlockMinecartEntity) return new ItemStack(Items.COMMAND_BLOCK_MINECART);

        return new ItemStack(Items.MINECART);
    }
}