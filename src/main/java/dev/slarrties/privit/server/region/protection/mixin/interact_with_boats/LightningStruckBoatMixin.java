package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(Entity.class)
public abstract class LightningStruckBoatMixin {

    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void preventLightningStrikeOnBoat(ServerLevel level, LightningBolt lightning, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Boat)) return;

        UUID responsible = WorldRegistry.get(level)
                .getTrackerManager()
                .getLightningOriginTracker()
                .getResponsible(lightning);

        if (responsible == null) {
            if (lightning.getCause() != null) {
                responsible = lightning.getCause().getUUID();
            }
        }
        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_BOATS, self.blockPosition(), level);
        if (!allowed) {
            ServerPlayer serverPlayer = level.getServer().getPlayerList().getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
            ci.cancel();
        }
    }
}