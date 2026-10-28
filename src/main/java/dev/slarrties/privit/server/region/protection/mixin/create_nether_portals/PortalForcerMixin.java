package dev.slarrties.privit.server.region.protection.mixin.create_nether_portals;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.NetherPortalEntryTracker;

import net.minecraft.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.PortalForcer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@AssociatedRule(Rule.CREATE_NETHER_PORTALS)
@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {

    @Final @Shadow
    private ServerLevel level;

    @Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
    private void preventCreatingPortalInProtectedRegion(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        if (level == null || level.isClientSide()) return;

        ServerPlayer serverPlayer = NetherPortalEntryTracker.getRecentPortalUser(level);
        if (serverPlayer == null) return;

        boolean allowed = WorldRegistry.get(level)
                .getRegionManager()
                .getAt(pos)
                .map(region -> region.isAllowed(serverPlayer.getUUID(), Rule.CREATE_NETHER_PORTALS))
                .orElse(true);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CREATE_NETHER_PORTAL, Color.RED);
            cir.setReturnValue(Optional.empty());
        }
    }
}