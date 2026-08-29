package dev.slarrties.privit.server.region.protection.mixin.push_entities;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.PUSH_ENTITIES)
@Mixin(Entity.class)
public abstract class PushEntityMixin {

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void preventPlayerPushingEntities(Entity pusher, CallbackInfo ci) {
        Entity victim = (Entity) (Object) this;

        if (victim.getWorld().isClient) return;
        if (!(pusher instanceof ServerPlayerEntity serverPlayer)) return;
        if (victim instanceof BoatEntity || victim instanceof AbstractMinecartEntity) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.PUSH_ENTITIES, victim.getBlockPos());

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PUSH_ENTITY, Color.RED);
            victim.setVelocity(0.0, victim.getVelocity().y, 0.0);
            ci.cancel();
        }

        InfluencedEntityTracker tracker = WorldRegistry.get((ServerWorld) victim.getWorld())
                .getTrackerManager()
                .getInfluencedEntityTracker();
        tracker.record(victim, serverPlayer.getUuid());
    }
}