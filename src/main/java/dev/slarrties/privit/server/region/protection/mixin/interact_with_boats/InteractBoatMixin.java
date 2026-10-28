package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(AbstractBoat.class)
public abstract class InteractBoatMixin {

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void onBoatPush(Entity pusher, CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat) (Object) this;
        if (boat.level().isClientSide()) return;

        if (pusher.level() instanceof ServerLevel serverLevel) {
            BoatOriginTracker boatOriginTracker = WorldRegistry.get(serverLevel).getTrackerManager().getBoatOriginTracker();
            boolean boatHasPassengers = !boat.getPassengers().isEmpty();

            if (pusher instanceof ServerPlayer serverPlayer) {
                boolean hasPermission = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_BOATS, boat.blockPosition(), serverLevel);
                if (boatHasPassengers) propogateResponsibleToEntity(serverPlayer.getUUID());

                if (!hasPermission) {
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
                    boat.setDeltaMovement(0.0, boat.getDeltaMovement().y, 0.0);
                    ci.cancel();
                    return;
                }
                boatOriginTracker.record(boat, serverPlayer.getUUID());
                return;
            }

            if (pusher instanceof Boat pusherBoat) {
                boatOriginTracker.propagate(pusherBoat, boat);
                UUID responsible = boatOriginTracker.getResponsible(boat);
                if (responsible != null) {
                    boolean forbidden = !RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_BOATS, boat.blockPosition(), serverLevel);
                    if (boatHasPassengers) propogateResponsibleToEntity(responsible);
                    if (forbidden) {
                        destroyIllegalBoat(boat, responsible, serverLevel);
                        ci.cancel();
                    }
                }
            }
        }
    }

    // TODO: updateVelocity in Entity class
    @Inject(method = "tick", at = @At("HEAD"))
    private void checkBoatMovement(CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat) (Object) this;
        if (boat.level().isClientSide()) return;
        if (boat.getDeltaMovement().horizontalDistanceSqr() < 0.0025) return;
        checkAndRemoveIfNotAllowed();
    }

    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void checkBoatFall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo ci) {
        AbstractBoat boat = (AbstractBoat) (Object) this;
        if (boat.level().isClientSide()) return;
        checkAndRemoveIfNotAllowed();
    }

    @Unique
    private void propogateResponsibleToEntity(UUID responsible) {
        AbstractBoat boat = (AbstractBoat) (Object) this;
        if (boat.level() instanceof ServerLevel serverLevel) {
            InfluencedEntityTracker entityTracker = WorldRegistry.get(serverLevel).getTrackerManager().getInfluencedEntityTracker();

            for (Entity entity : boat.getPassengers()) {
                if (!(entity instanceof ServerPlayer)) {
                    entityTracker.record(entity, responsible);
                }
            }
        }
    }

    @Unique
    private void checkAndRemoveIfNotAllowed() {
        AbstractBoat boat = (AbstractBoat) (Object) this;
        if (boat.level() instanceof ServerLevel serverLevel) {
            BoatOriginTracker boatOriginTracker = WorldRegistry.get(serverLevel).getTrackerManager().getBoatOriginTracker();
            UUID responsible = boatOriginTracker.getResponsible(boat);
            if (responsible == null) return;
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_BOATS, boat.blockPosition(), serverLevel)) {
                destroyIllegalBoat(boat, responsible, serverLevel);
            }
        }
    }

    @Unique
    private void destroyIllegalBoat(AbstractBoat boat, UUID player, ServerLevel serverLevel) {
        boat.ejectPassengers();
//        ItemStack boatItem = boat.getPickResult();
//        boat.spawnAtLocation(boatItem);
        boat.discard();
        ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(player);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
        WorldRegistry.get(serverLevel).getTrackerManager().getBoatOriginTracker().remove(boat);
    }
}