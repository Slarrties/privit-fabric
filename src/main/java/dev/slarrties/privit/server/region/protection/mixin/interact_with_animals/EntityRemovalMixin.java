package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRemovalMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void onEntityRemoved(RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        tracker.remove(entity);
    }
}