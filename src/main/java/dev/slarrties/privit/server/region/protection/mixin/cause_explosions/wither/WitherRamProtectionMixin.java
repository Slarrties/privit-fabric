package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherEntity.class)
public abstract class WitherRamProtectionMixin {

    @Inject(
            method = "mobTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;)Z",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void preventWitherRamDestruction(CallbackInfo ci) {
        WitherEntity wither = (WitherEntity) (Object) this;

        if(wither.getWorld() instanceof ServerWorld serverWorld) {
            UUID responsible = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .getResponsiblePlayer(wither);

            if (responsible == null) return;
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, wither.getBlockPos(), serverWorld)) {
                ServerPlayerEntity serverPlayer = serverWorld.getServer().getPlayerManager().getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
                ci.cancel();
            }
        }
    }
}