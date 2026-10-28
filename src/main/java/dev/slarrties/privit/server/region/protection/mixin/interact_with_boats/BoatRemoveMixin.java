package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(AbstractBoat.class)
public abstract class BoatRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onBoatRemove(RemovalReason reason, CallbackInfo ci) {
        Boat boat = (Boat) (Object) this;

        if (boat.level() instanceof ServerLevel serverLevel) {
            WorldRegistry.get(serverLevel).getTrackerManager()
                    .getBoatOriginTracker()
                    .remove(boat);
        }
    }
}