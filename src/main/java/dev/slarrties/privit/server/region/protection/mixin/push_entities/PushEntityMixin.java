package dev.slarrties.privit.server.region.protection.mixin.push_entities;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.PUSH_ENTITIES)
@Mixin(Entity.class)
public abstract class PushEntityMixin {

    @Inject(method = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void preventPlayerPushingEntities(Entity pusher, CallbackInfo ci) {
        Entity victim = (Entity) (Object) this;
        if (victim.level().isClientSide()) return;
        if (!(pusher instanceof ServerPlayer serverPlayer)) return;
        if (victim instanceof Boat || victim instanceof AbstractMinecart) return;

        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.PUSH_ENTITIES, victim.blockPosition(), serverPlayer.level());
        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PUSH_ENTITY, Color.RED);
            victim.setDeltaMovement(0.0, victim.getDeltaMovement().y, 0.0);
            ci.cancel();
            return;
        }

        if (victim.level() instanceof ServerLevel serverLevel) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            tracker.record(victim, serverPlayer.getUUID());
        }
    }
}