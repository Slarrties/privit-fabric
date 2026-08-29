package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule({Rule.INTERACT_WITH_MINECARTS, Rule.CAUSE_EXPLOSIONS})
@Mixin(TntMinecartEntity.class)
public abstract class TntMinecartIgnitionMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onTntMinecartDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        TntMinecartEntity minecart = (TntMinecartEntity) (Object) this;
        World world = minecart.getWorld();

        if(world instanceof ServerWorld serverWorld) {
            UUID responsible = findResponsiblePlayer(source, world, minecart);
            if (responsible == null) return;

            boolean canDamage = RegionPermissionChecker.isAllowed(responsible, Rule.INTERACT_WITH_MINECARTS, minecart.getBlockPos(), serverWorld);
            if (!canDamage) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer()
                        .getPlayerManager()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_MINECART, Color.RED);
                cir.cancel();
                return;
            }

            if (isIgnitionAttempt(source)) {
                WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getMinecartOriginTracker()
                        .record(minecart, responsible);

                WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getExplosionOriginTracker()
                        .record(minecart, responsible);
            }
        }
    }

    @Unique
    private UUID findResponsiblePlayer(DamageSource source, World world, TntMinecartEntity minecart) {
        if (source.getAttacker() instanceof ServerPlayerEntity player) return player.getUuid();

        Entity sourceEntity = source.getSource();

        if (sourceEntity instanceof PersistentProjectileEntity || sourceEntity instanceof SmallFireballEntity) {
            if (sourceEntity instanceof ProjectileEntity projectile) {
                if (projectile.getOwner() instanceof ServerPlayerEntity owner) {
                    return owner.getUuid();
                }
            }
            if (world instanceof ServerWorld serverWorld) {
                return RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, minecart.getBlockPos());
            }
        }
        return null;
    }

    @Unique
    private boolean isIgnitionAttempt(DamageSource source) {
        Entity sourceEntity = source.getSource();
        return sourceEntity instanceof PersistentProjectileEntity p && p.isOnFire() || sourceEntity instanceof SmallFireballEntity;
    }
}