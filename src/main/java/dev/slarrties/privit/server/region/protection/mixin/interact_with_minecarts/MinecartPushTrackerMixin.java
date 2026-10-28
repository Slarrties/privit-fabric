package dev.slarrties.privit.server.region.protection.mixin.interact_with_minecarts;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.MinecartFuelTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.INTERACT_WITH_MINECARTS)
@Mixin(AbstractMinecart.class)
public abstract class MinecartPushTrackerMixin {

    @Inject(method = "push", at = @At("HEAD"))
    private void trackMinecartPush(Entity entity, CallbackInfo ci) {
        AbstractMinecart minecart = (AbstractMinecart) (Object) this;
        if (minecart.level() instanceof ServerLevel serverLevel) {
            MinecartOriginTracker minecartOriginTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getMinecartOriginTracker();
            MinecartFuelTracker minecartFuelTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getMinecartFuelTracker();

            if (entity instanceof ServerPlayer player) {
                minecartOriginTracker.record(minecart, player.getUUID());
                this.propogateResponsibleToEntity(player.getUUID());
            } else if (entity instanceof MinecartFurnace furnaceCart) {
                UUID responsible = minecartFuelTracker.getResponsible(furnaceCart);
                if (responsible != null) {
                    minecartOriginTracker.record(minecart, responsible);
                    this.propogateResponsibleToEntity(responsible);
                }
            } else if (entity instanceof AbstractMinecart otherCart) {
                minecartOriginTracker.propagate(otherCart, minecart);
                UUID responsible = minecartOriginTracker.getResponsible(otherCart);
                if (responsible != null) this.propogateResponsibleToEntity(responsible);
            }
        }
    }

    @Unique
    private void propogateResponsibleToEntity(UUID responsible) {
        AbstractMinecart minecart = (AbstractMinecart) (Object) this;
        if (minecart.level() instanceof ServerLevel serverLevel) {
            InfluencedEntityTracker entityTracker = WorldRegistry.get(serverLevel).getTrackerManager().getInfluencedEntityTracker();
            for (Entity entity : minecart.getPassengers()) {
                if (!(entity instanceof ServerPlayer)) {
                    entityTracker.record(entity, responsible);
                }
            }
        }
    }
}