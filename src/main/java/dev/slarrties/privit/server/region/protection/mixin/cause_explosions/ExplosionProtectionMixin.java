package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule({
        Rule.CAUSE_EXPLOSIONS,
        Rule.THROW_WIND_CHARGES,
        Rule.CAUSE_BLOCK_FALL
})
@Mixin(Level.class)
public abstract class ExplosionProtectionMixin {

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("HEAD"), cancellable = true)
    private void onExplode1(Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        handleExplodeHead(source, x, y, z, ci);
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("HEAD"), cancellable = true)
    private void onExplode2(Entity source, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        handleExplodeHead(source, x, y, z, ci);
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("HEAD"), cancellable = true)
    private void onExplode3(Entity source, DamageSource damageSource, ExplosionDamageCalculator calculator, Vec3 boomPos, float radius, boolean fire, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        handleExplodeHead(source, boomPos.x, boomPos.y, boomPos.z, ci);
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("HEAD"), cancellable = true)
    private void onExplode4(Entity source, DamageSource damageSource, ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        handleExplodeHead(source, x, y, z, ci);
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("RETURN"))
    private void onExplodeReturn1(Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("RETURN"))
    private void onExplodeReturn2(Entity source, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("RETURN"))
    private void onExplodeReturn3(Entity source, DamageSource damageSource, ExplosionDamageCalculator calculator, Vec3 boomPos, float radius, boolean fire, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V", at = @At("RETURN"))
    private void onExplodeReturn4(Entity source, DamageSource damageSource, ExplosionDamageCalculator calculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        BlockFallContext.pop();
    }

    @Unique
    private void handleExplodeHead(Entity source, double x, double y, double z, CallbackInfo ci) {
        if (!((Object) this instanceof ServerLevel serverLevel)) return;
        UUID responsible = resolveResponsible(source, serverLevel);
        if (responsible == null) return;

        BlockPos pos = BlockPos.containing(x, y, z);
        Rule rule = getRuleToCheck(source);
        if (!RegionPermissionChecker.isAllowed(responsible, rule, pos, serverLevel)) {
            sendDenyNotification(responsible, rule, serverLevel);
            ci.cancel();
            return;
        }
        BlockFallContext.push(responsible, pos);
    }

    @Unique
    private UUID resolveResponsible(Entity source, ServerLevel serverLevel) {
        if (source == null) return null;

        ExplosionOriginTracker tracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getExplosionOriginTracker();
        UUID responsible = tracker.getResponsible(source);

        if (responsible != null) return responsible;
        if (source instanceof ServerPlayer player) return player.getUUID();
        if (source instanceof WitherSkull skull) {
            Entity owner = skull.getOwner();
            if (owner instanceof WitherBoss wither) {
                responsible = tracker.getResponsible(wither);
                if (responsible != null) return responsible;
            }
        }
        if (source instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner instanceof ServerPlayer player) return player.getUUID();
            if (owner != null) {
                responsible = tracker.getResponsible(owner);
                if (responsible != null) return responsible;
            }
        }
        return null;
    }

    @Unique
    private Rule getRuleToCheck(Entity source) {
        return (source instanceof AbstractWindCharge)
                ? Rule.THROW_WIND_CHARGES
                : Rule.CAUSE_EXPLOSIONS;
    }

    @Unique
    private void sendDenyNotification(UUID responsible, Rule rule, ServerLevel serverLevel) {
        NotificationType type = (rule == Rule.THROW_WIND_CHARGES)
                ? NotificationType.DENY_THROW_WIND_CHARGE
                : NotificationType.DENY_CAUSE_EXPLOSION;
        ServerPlayer serverPlayer = serverLevel.getServer()
                .getPlayerList()
                .getPlayer(responsible);
        PlayerNotification.trySend(serverPlayer, type, Color.RED);
    }
}