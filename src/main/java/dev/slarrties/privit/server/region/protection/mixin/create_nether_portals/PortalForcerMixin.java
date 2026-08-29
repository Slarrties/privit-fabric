package dev.slarrties.privit.server.region.protection.mixin.create_nether_portals;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.NetherPortalEntryTracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.dimension.PortalForcer;

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

    @Final @Shadow private ServerWorld world;

    @Inject(method = "createPortal", at = @At("HEAD"), cancellable = true)
    private void preventCreatingPortalInProtectedRegion(
            BlockPos pos,
            Direction.Axis axis,
            CallbackInfoReturnable<Optional<BlockLocating.Rectangle>> cir) {
        if (world == null || world.isClient) return;

        ServerPlayerEntity serverPlayer = NetherPortalEntryTracker.getRecentPortalUser(world);
        if (serverPlayer == null) return;

        boolean allowed = WorldRegistry.get(world)
                .getRegionManager()
                .getAt(pos)
                .map(region -> region.isAllowed(serverPlayer.getUuid(), Rule.CREATE_NETHER_PORTALS))
                .orElse(true);

        if (!allowed) {
            cir.setReturnValue(Optional.empty());
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_CREATE_NETHER_PORTAL, Color.RED);
        }
    }
}