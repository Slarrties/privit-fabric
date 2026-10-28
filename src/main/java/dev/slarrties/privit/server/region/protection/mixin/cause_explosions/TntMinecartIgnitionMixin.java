package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule({Rule.INTERACT_WITH_MINECARTS, Rule.CAUSE_EXPLOSIONS})
@Mixin(MinecartTNT.class)
public abstract class TntMinecartIgnitionMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void onTntMinecartDamage(ServerLevel serverLevel, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        MinecartTNT minecart = (MinecartTNT) (Object) this;
        UUID responsible = findResponsiblePlayer(source, serverLevel, minecart);
        if (responsible == null) return;

        boolean canDamage = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, minecart.blockPosition(), serverLevel);
        if (!canDamage) {
            ServerPlayer serverPlayer = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(responsible);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
            cir.setReturnValue(false);
            return;
        }

        if (isIgnitionAttempt(source)) {
            WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getMinecartOriginTracker()
                    .record(minecart, responsible);
            WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .record(minecart, responsible);
        }

    }

    @Unique
    private UUID findResponsiblePlayer(DamageSource source, Level level, MinecartTNT minecart) {
        if (source.getEntity() instanceof ServerPlayer player) return player.getUUID();

        Entity sourceEntity = source.getDirectEntity();
        if (sourceEntity instanceof Projectile projectile) {
            if (projectile.getOwner() instanceof ServerPlayer owner) {
                return owner.getUUID();
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            return RedstoneReceiverHandler.findResponsiblePlayer(serverLevel, minecart.blockPosition());
        }
        return null;
    }

    @Unique
    private boolean isIgnitionAttempt(DamageSource source) {
        Entity sourceEntity = source.getDirectEntity();
        return sourceEntity instanceof Projectile p && p.isOnFire();
    }
}