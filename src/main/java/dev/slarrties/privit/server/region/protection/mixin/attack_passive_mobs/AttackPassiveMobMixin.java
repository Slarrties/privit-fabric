package dev.slarrties.privit.server.region.protection.mixin.attack_passive_mobs;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.ATTACK_PASSIVE_MOBS)
@Mixin(LivingEntity.class)
public abstract class AttackPassiveMobMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventPassiveDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) return;
        if (!isPassive(target)) return;

        UUID responsiblePlayerUuid = getResponsibleAttacker(source, level);
        if (responsiblePlayerUuid == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsiblePlayerUuid, Rule.ATTACK_PASSIVE_MOBS, target.blockPosition(), level);
        if (!allowed) {
            cir.setReturnValue(false);
            if (source.getDirectEntity() instanceof ServerPlayer serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_ATTACK_PASSIVE_MOB, Color.RED);
            }
        }
    }

    @Unique
    private UUID getResponsibleAttacker(DamageSource source, ServerLevel level) {
        Entity attacker = source.getDirectEntity();
        if (attacker == null) return null;
        if (attacker instanceof ServerPlayer player) return player.getUUID();

        if (attacker instanceof Mob mob) {
            InfluencedEntityTracker tracker = WorldRegistry.get(level)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            return tracker.getResponsible(mob);
        }

        if (attacker instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer player) return player.getUUID();
            if (owner instanceof Mob mob) {
                InfluencedEntityTracker tracker = WorldRegistry.get(level)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                return tracker.getResponsible(mob);
            }
        }
        return null;
    }

    @Unique
    private static boolean isPassive(LivingEntity entity) {
        return entity instanceof Animal ||
                entity instanceof Villager ||
                entity instanceof Allay ||
                entity instanceof Axolotl ||
                entity instanceof WanderingTrader;
    }
}