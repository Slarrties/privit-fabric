package dev.slarrties.privit.server.region.protection.mixin.use_leashes;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.PlayerPermissionCache;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Leashable;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.USE_LEASHES)
@Mixin(ServerPlayerEntity.class)
public abstract class LeashBreakMixin {

    @Unique
    private int leashCheckCooldown = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void breakLeashesInForbiddenRegion(CallbackInfo ci) {
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) (Object) this;

        if (serverPlayer.getWorld().isClient) return;
        if (++leashCheckCooldown < 8) return;
        leashCheckCooldown = 0;

        boolean hasAnyLeash = false;
        for (Entity entity : serverPlayer.getWorld().getOtherEntities(serverPlayer, serverPlayer.getBoundingBox().expand(20))) {
            if (entity instanceof Leashable leashable && leashable.getLeashHolder() == serverPlayer) {
                hasAnyLeash = true;
                break;
            }
        }
        if (!hasAnyLeash) return;

        boolean allowed = PlayerPermissionCache.isAllowed(serverPlayer, Rule.USE_LEASHES, serverPlayer.getBlockPos());
        if (allowed) return;

        for (Entity entity : serverPlayer.getWorld().getOtherEntities(serverPlayer, serverPlayer.getBoundingBox().expand(20))) {
            if (entity instanceof Leashable leashable && leashable.getLeashHolder() == serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_USE_LEASH, Color.RED);
                leashable.detachLeash(true, true);
            }
        }
    }
}