package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.MinecartFuelTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartPushTrackerMixin {

    @Inject(method = "pushAwayFrom", at = @At("HEAD"))
    private void trackMinecartPush(Entity entity, CallbackInfo ci) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;

        if (minecart.getWorld() instanceof ServerWorld serverWorld) {
            MinecartOriginTracker minecartOriginTracker = WorldRegistry.get(serverWorld).getTrackerManager().getMinecartOriginTracker();
            MinecartFuelTracker minecartFuelTracker = WorldRegistry.get(serverWorld).getTrackerManager().getMinecartFuelTracker();

            if (entity instanceof ServerPlayerEntity player) {
                minecartOriginTracker.record(minecart, player.getUuid());
                this.propogateResponsibleToEntity(player.getUuid());
            } else if (entity instanceof FurnaceMinecartEntity furnaceCart) {
                UUID responsible = minecartFuelTracker.getResponsiblePlayer(furnaceCart);

                if (responsible != null) {
                    minecartOriginTracker.record(minecart, responsible);
                    this.propogateResponsibleToEntity(responsible);
                }
            } else if (entity instanceof AbstractMinecartEntity otherCart) {
                minecartOriginTracker.propagate(otherCart, minecart);
                UUID responsible = minecartOriginTracker.getResponsiblePlayer(otherCart);
                if (responsible != null) this.propogateResponsibleToEntity(responsible);
            }
        }
    }

    @Unique
    private void propogateResponsibleToEntity(UUID responsible) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;

        if(minecart.getWorld() instanceof ServerWorld serverWorld) {
            InfluencedEntityTracker entityTracker = WorldRegistry.get(serverWorld).getTrackerManager().getInfluencedEntityTracker();

            for (Entity entity : minecart.getPassengerList()) {
                if (!(entity instanceof ServerPlayerEntity)) {
                    entityTracker.record(entity, responsible);
                }
            }
        }
    }
}