package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// shouldn't it be in this package?
@Mixin(LivingEntity.class)
public abstract class EntityDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onEntityDamaged(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.getWorld().isClient) return;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        if (!(target instanceof MobEntity)) return;
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();

            tracker.record(target, player.getUuid());
        }
    }
}