package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(BoatEntity.class)
public abstract class BoatRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onBoatRemove(RemovalReason reason, CallbackInfo ci) {
        BoatEntity boat = (BoatEntity) (Object) this;
        if(boat.getWorld() instanceof ServerWorld serverWorld) {
            WorldRegistry.get(serverWorld).getTrackerManager().getBoatOriginTracker().remove(boat);
        }
    }
}