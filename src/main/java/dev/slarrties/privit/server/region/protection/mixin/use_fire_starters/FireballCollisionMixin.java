package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.redstone.handler.RedstoneReceiverHandler;

import net.minecraft.world.World;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(SmallFireballEntity.class)
public abstract class FireballCollisionMixin {

    @Inject(method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void cancelFireChargeInProtectedRegion(HitResult hitResult, CallbackInfo ci) {
        SmallFireballEntity fireball = (SmallFireballEntity) (Object) this;
        if (fireball.getWorld().isClient) return;

        Entity owner = fireball.getOwner();
        UUID responsible;
        World world = fireball.getWorld();
        BlockPos impactPos = BlockPos.ofFloored(hitResult.getPos());

        if(world instanceof ServerWorld serverWorld) {
            if (owner instanceof ServerPlayerEntity serverPlayer) {
                responsible = serverPlayer.getUuid();
            } else {
                responsible = RedstoneReceiverHandler.findResponsiblePlayer(serverWorld, impactPos);
            }

            if (responsible == null) return;

            boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.USE_FIRE_STARTERS, impactPos, serverWorld);

            if (!allowed) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer()
                        .getPlayerManager()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_FIRE_STARTER, Color.RED);
                fireball.discard();
                ci.cancel();
            } else {
                FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                        .getTrackerManager()
                        .getFireOriginTracker();
                fireTracker.record(this.calculateFirePlacementPos(hitResult), responsible);
            }
        }
    }

    @Unique
    private BlockPos calculateFirePlacementPos(HitResult hitResult) {
        if (!(hitResult instanceof BlockHitResult blockHit))
            return BlockPos.ofFloored(hitResult.getPos());

        BlockPos hitBlock = blockHit.getBlockPos();
        Direction side = blockHit.getSide();

        return hitBlock.offset(side);
    }
}