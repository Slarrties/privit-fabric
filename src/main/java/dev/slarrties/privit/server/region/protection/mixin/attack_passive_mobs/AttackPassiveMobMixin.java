package dev.slarrties.privit.server.region.protection.mixin.attack_passive_mobs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.ATTACK_PASSIVE_MOBS)
@Mixin(LivingEntity.class)
public abstract class AttackPassiveMobMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventPassiveDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;

        if (target.getWorld().isClient || !(target.getWorld() instanceof ServerWorld serverWorld)) return;
        if (!isPassive(target)) return;

        UUID responsiblePlayerUuid = getResponsibleAttacker(source, (ServerWorld) target.getWorld());
        if (responsiblePlayerUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsiblePlayerUuid, Rule.ATTACK_PASSIVE_MOBS, target.getBlockPos(), serverWorld);

        if (!allowed) {
            cir.setReturnValue(false);

            if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ATTACK_PASSIVE_MOB, Color.RED);
            }
        }
    }

    @Unique
    private UUID getResponsibleAttacker(DamageSource source, ServerWorld world) {
        Entity attacker = source.getAttacker();
        if (attacker == null) return null;

        if (attacker instanceof ServerPlayerEntity player) {
            return player.getUuid();
        }

        if (attacker instanceof MobEntity) {
            InfluencedEntityTracker tracker = WorldRegistry.get(world)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            return tracker.getResponsible(attacker);
        }

        if (attacker instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();

            if (owner instanceof ServerPlayerEntity player) return player.getUuid();
            if (owner instanceof MobEntity) {
                InfluencedEntityTracker tracker = WorldRegistry.get(world)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                return tracker.getResponsible(owner);
            }
        }

        return null;
    }

    @Unique
    private static boolean isPassive(LivingEntity entity) {
        return entity instanceof AnimalEntity ||
                entity instanceof VillagerEntity ||
                entity instanceof AllayEntity ||
                entity instanceof AxolotlEntity ||
                entity instanceof WanderingTraderEntity;
    }
}