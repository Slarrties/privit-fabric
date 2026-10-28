package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.MinecartFuelTracker;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(MinecartFurnace.class)
public abstract class FurnaceMinecartProtectionMixin {

    @Shadow
    private int fuel;

    // TODO: tick head
    @Inject(method = "tick", at = @At("HEAD"))
    private void checkPoweredMinecartInRegion(CallbackInfo ci) {
        MinecartFurnace minecart = (MinecartFurnace) (Object) this;
        if (this.fuel <= 0) return;

        if (minecart.level() instanceof ServerLevel serverLevel) {
            MinecartFuelTracker minecartFuelTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getMinecartFuelTracker();
            UUID responsible = minecartFuelTracker.getResponsible(minecart);
            if (responsible == null) return;

            BlockPos currentPos = minecart.blockPosition();
            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, currentPos, serverLevel);
            if (!allowed) {
                ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                minecartFuelTracker.remove(minecart);
                minecart.discard();
            }
        }
    }
}