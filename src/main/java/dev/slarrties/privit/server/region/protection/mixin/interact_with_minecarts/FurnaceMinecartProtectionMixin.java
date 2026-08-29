package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.MinecartFuelTracker;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(FurnaceMinecartEntity.class)
public abstract class FurnaceMinecartProtectionMixin {

    @Shadow
    private int fuel;

    @Inject(method = "moveOnRail", at = @At("HEAD"))
    private void checkPoweredMinecartInRegion(BlockPos pos, BlockState state, CallbackInfo ci) {
        FurnaceMinecartEntity minecart = (FurnaceMinecartEntity) (Object) this;

        if (this.fuel <= 0) return;
        if (minecart.getWorld() instanceof ServerWorld serverWorld) {
            MinecartFuelTracker minecartFuelTracker = WorldRegistry.get(serverWorld).getTrackerManager().getMinecartFuelTracker();
            UUID responsible = minecartFuelTracker.getResponsiblePlayer(minecart);
            if (responsible == null) return;

            BlockPos currentPos = minecart.getBlockPos();
            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, currentPos, serverWorld);

            if (!allowed) {
                minecart.discard();
                minecartFuelTracker.remove(minecart);

                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            }
        }
    }
}