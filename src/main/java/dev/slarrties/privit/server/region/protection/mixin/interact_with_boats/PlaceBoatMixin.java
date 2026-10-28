package dev.slarrties.privit.server.region.protection.mixin.interact_with_boats;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(BoatItem.class)
public abstract class PlaceBoatMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventBoatPlacement(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        BlockHitResult hitResult = level.clip(new ClipContext(
                serverPlayer.getEyePosition(1.0F),
                serverPlayer.getEyePosition(1.0F).add(serverPlayer.getLookAngle().scale(5.0)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                serverPlayer
        ));
        BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.INTERACT_WITH_BOATS, placePos, serverPlayer.level());

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void recordPlacedBoat(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (level instanceof ServerLevel serverLevel) {
            BlockHitResult hitResult = level.clip(new ClipContext(
                    serverPlayer.getEyePosition(1.0F),
                    serverPlayer.getEyePosition(1.0F).add(serverPlayer.getLookAngle().scale(5.0)),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY,
                    serverPlayer
            ));
            BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());
            BoatOriginTracker boatTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getBoatOriginTracker();
            AABB searchBox = new AABB(placePos).inflate(4.0);

            for (Boat boat : level.getEntitiesOfClass(Boat.class, searchBox, b -> true)) {
                boolean hasOwner = boatTracker.getResponsible(boat) != null;

                if (!hasOwner) {
                    boatTracker.record(boat, serverPlayer.getUUID());
                }
            }
        }
    }
}