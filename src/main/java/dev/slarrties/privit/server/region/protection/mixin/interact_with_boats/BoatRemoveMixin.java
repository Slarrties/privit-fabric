package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(Entity.class)
public abstract class BoatRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onEntityRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (self instanceof BoatEntity boat) {
            if (self.getWorld() instanceof ServerWorld serverWorld) {
                WorldRegistry.get(serverWorld).getTrackerManager().getBoatOriginTracker().remove(boat);
            }
        }
    }
}