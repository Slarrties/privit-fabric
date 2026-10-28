package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.vehicle.minecart.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecart.class)
public abstract class MinecartProtectionMixin {

    // TODO: tick head
    @Inject(method = "tick", at = @At("HEAD"))
    private void checkMinecartMovement(CallbackInfo ci) {
        checkAndRemoveIfNotAllowed();
    }

    @Unique
    private void checkAndRemoveIfNotAllowed() {
        AbstractMinecart minecart = (AbstractMinecart) (Object) this;
        if (minecart.level() instanceof ServerLevel serverLevel) {
            MinecartOriginTracker minecartOriginTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getMinecartOriginTracker();
            UUID responsible = minecartOriginTracker.getResponsible(minecart);
            if (responsible == null) return;

            BlockPos currentPos = minecart.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, currentPos, serverLevel);
            if (!allowed) {
                ItemStack stack = getMinecartItemStack(minecart);
                ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);

                minecart.spawnAtLocation(serverLevel, stack);
                minecartOriginTracker.remove(minecart);
                minecart.discard();
            }
        }
    }

    @Unique
    private ItemStack getMinecartItemStack(AbstractMinecart minecart) {
        if (minecart instanceof MinecartHopper) return new ItemStack(Items.HOPPER_MINECART);
        if (minecart instanceof MinecartChest) return new ItemStack(Items.CHEST_MINECART);
        if (minecart instanceof MinecartFurnace) return new ItemStack(Items.FURNACE_MINECART);
        if (minecart instanceof MinecartTNT) return new ItemStack(Items.TNT_MINECART);
        if (minecart instanceof MinecartCommandBlock) return new ItemStack(Items.COMMAND_BLOCK_MINECART);
        return new ItemStack(Items.MINECART);
    }
}