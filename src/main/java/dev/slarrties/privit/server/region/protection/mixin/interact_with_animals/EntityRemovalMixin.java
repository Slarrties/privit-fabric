package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRemovalMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onEntityRemoved(RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.getWorld().isClient) return;
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return;

        InfluencedEntityTracker tracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        tracker.remove(entity);
    }
}