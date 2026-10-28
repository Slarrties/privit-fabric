package dev.slarrties.privit.server.region.protection.mixin.cause_explosions.wither;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_EXPLOSIONS)
@Mixin(WitherBoss.class)
public abstract class WitherRamProtectionMixin {

    @Inject(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void preventWitherRamDestruction(CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;
        if (wither.level() instanceof ServerLevel serverLevel) {
            UUID responsible = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker()
                    .getResponsible(wither);
            if (responsible == null) return;
            if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_EXPLOSIONS, wither.blockPosition(), serverLevel)) {
                ServerPlayer serverPlayer = serverLevel.getServer()
                        .getPlayerList()
                        .getPlayer(responsible);
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CAUSE_EXPLOSION, Color.RED);
                ci.cancel();
            }
        }
    }
}