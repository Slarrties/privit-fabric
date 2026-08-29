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

import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(BoatEntity.class)
public abstract class InteractBoatMixin {

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void onBoatPushAwayFrom(Entity pusher, CallbackInfo ci) {
        BoatEntity boat = (BoatEntity) (Object) this;

        if (boat.getWorld().isClient) return;
        if (pusher.getWorld() instanceof ServerWorld serverWorld) {
            BoatOriginTracker boatOriginTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getBoatOriginTracker();
            boolean boatHasPassengers = boat.hasPassengers();

            if (pusher instanceof ServerPlayerEntity serverPlayer) {
                boolean hasPermission = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, boat.getBlockPos());

                if (boatHasPassengers) propogateResponsibleToEntity(serverPlayer.getUuid());
                if (!hasPermission) {
                    PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
                    boat.setVelocity(0.0, boat.getVelocity().y, 0.0);
                    ci.cancel();
                    return;
                }

                boatOriginTracker.record(boat, serverPlayer.getUuid());
                return;
            }

            if (pusher instanceof BoatEntity pusherBoat) {
                boatOriginTracker.propagate(pusherBoat, boat);
                UUID responsible = boatOriginTracker.getResponsiblePlayer(boat);

                if (responsible != null) {
                    boolean forbidden = !RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_BOATS, boat.getBlockPos(), serverWorld);

                    if (boatHasPassengers) propogateResponsibleToEntity(responsible);
                    if (forbidden) {
                        destroyIllegalBoat(boat, responsible, serverWorld);
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "updateVelocity", at = @At("HEAD"))
    private void checkBoatMovement(CallbackInfo ci) {
        BoatEntity boat = (BoatEntity) (Object) this;
        if (boat.getWorld().isClient) return;
        if (boat.getVelocity().horizontalLengthSquared() < 0.0025) return;

        checkAndRemoveIfNotAllowed();
    }

    @Inject(method = "fall", at = @At("HEAD"))
    private void checkBoatFall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo ci) {
        BoatEntity boat = (BoatEntity) (Object) this;
        if (boat.getWorld().isClient) return;

        checkAndRemoveIfNotAllowed();
    }

    @Unique
    private void propogateResponsibleToEntity(UUID responsible) {
        BoatEntity boat = (BoatEntity) (Object) this;

        if(boat.getWorld() instanceof ServerWorld serverWorld) {
            InfluencedEntityTracker entityTracker = WorldRegistry.get(serverWorld).getTrackerManager().getInfluencedEntityTracker();

            for (Entity entity : boat.getPassengerList()) {
                if (!(entity instanceof ServerPlayerEntity)) {
                    entityTracker.record(entity, responsible);
                }
            }
        }
    }

    @Unique
    private void checkAndRemoveIfNotAllowed() {
        BoatEntity boat = (BoatEntity) (Object) this;

        if(boat.getWorld() instanceof ServerWorld serverWorld) {
            BoatOriginTracker boatOriginTracker = WorldRegistry.get(serverWorld).getTrackerManager().getBoatOriginTracker();
            UUID responsible = boatOriginTracker.getResponsiblePlayer(boat);

            if (responsible == null) return;
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_BOATS, boat.getBlockPos(), serverWorld)) {
                destroyIllegalBoat(boat, responsible, serverWorld);
            }
        }
    }

    @Unique
    private void destroyIllegalBoat(BoatEntity boat, UUID player, ServerWorld serverWorld) {
        boat.removeAllPassengers();

        ItemStack boatItem = boat.asItem().getDefaultStack();
        boat.dropStack(boatItem);
        boat.discard();

        ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(player);
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
        WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getBoatOriginTracker()
                .remove(boat);
    }
}