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

import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.item.BoatItem;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.INTERACT_WITH_BOATS)
@Mixin(BoatItem.class)
public abstract class PlaceBoatMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventBoatPlacement(World world, PlayerEntity user, Hand hand,
                                      CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                serverPlayer.getCameraPosVec(1.0F),
                serverPlayer.getCameraPosVec(1.0F).add(serverPlayer.getRotationVector().multiply(5.0)),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.ANY,
                serverPlayer
        ));

        BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
        boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.INTERACT_WITH_BOATS, placePos);

        if (!allowed) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_INTERACT_BOAT, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            cir.setReturnValue(TypedActionResult.fail(serverPlayer.getStackInHand(hand)));
        }
    }

    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void recordPlacedBoat(World world, PlayerEntity user, Hand hand,
                                  CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;
        if (world instanceof ServerWorld serverWorld) {
            BlockHitResult hitResult = world.raycast(new RaycastContext(
                    serverPlayer.getCameraPosVec(1.0F),
                    serverPlayer.getCameraPosVec(1.0F).add(serverPlayer.getRotationVector().multiply(5.0)),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.ANY,
                    serverPlayer
            ));

            BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
            BoatOriginTracker boatTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getBoatOriginTracker();
            var searchBox = new Box(placePos).expand(4.0); // large area?

            for (BoatEntity boat : world.getEntitiesByClass(BoatEntity.class, searchBox, b -> true)) {
                boolean hasOwner = boatTracker.getResponsiblePlayer(boat) != null;

                if (!hasOwner) {
                    boatTracker.record(boat, serverPlayer.getUuid());
                }
            }
        }
    }
}