package dev.slarrties.privit.server.region.protection.mixin.interact_with_animals;

import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EntityDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void onEntityDamaged(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;
        if (!(target instanceof Mob)) return;
        if (target.level() instanceof ServerLevel serverLevel) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            tracker.record(target, player.getUUID());
        }
    }
}