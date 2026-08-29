package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(Entity.class)
public abstract class LightningStruckBoatMixin {

    @Inject(method = "onStruckByLightning", at = @At("HEAD"), cancellable = true)
    private void preventLightningStrikeOnBoat(ServerWorld serverWorld, LightningEntity lightning, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof BoatEntity)) return;

        UUID responsible = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getLightningOriginTracker()
                .getResponsible(lightning);
        if (responsible == null) {
            if (lightning.getChanneler() != null)
                responsible = lightning.getChanneler().getUuid();
        }
        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_BOATS, self.getBlockPos(), serverWorld);

        if (!allowed) {
            ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
            ci.cancel();
        }
    }
}