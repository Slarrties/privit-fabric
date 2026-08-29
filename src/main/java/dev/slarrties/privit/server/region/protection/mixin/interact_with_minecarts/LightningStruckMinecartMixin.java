package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(Entity.class)
public abstract class LightningStruckMinecartMixin {

    @Inject(method = "onStruckByLightning", at = @At("HEAD"), cancellable = true)
    private void preventLightningStrikeOnMinecart(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof AbstractMinecartEntity)) return;
        if (self.getWorld() instanceof ServerWorld serverWorld) {
            UUID responsible = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getLightningOriginTracker()
                    .getResponsible(lightning);
            if (responsible == null) {
                if (lightning.getChanneler() != null)
                    responsible = lightning.getChanneler().getUuid();
            }
            if (responsible == null) return;

            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, self.getBlockPos(), serverWorld);

            if (!allowed) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                ci.cancel();
            }
        }
    }
}