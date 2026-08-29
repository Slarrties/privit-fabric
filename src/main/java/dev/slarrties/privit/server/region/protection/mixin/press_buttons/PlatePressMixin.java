package dev.slarrties.privit.server.region.protection.mixin.press_buttons;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.BoatOriginTracker;
import dev.slarrties.privit.server.tracking.protection.MinecartOriginTracker;
import dev.slarrties.privit.server.tracking.protection.RedstoneOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.PRESS_BUTTONS)
@Mixin(AbstractPressurePlateBlock.class)
public abstract class PlatePressMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void preventPlateActivation(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;

        UUID responsible = getResponsiblePlayer(entity, serverWorld);
        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.PRESS_BUTTONS, pos, serverWorld);

        if (!allowed) {
            if (entity instanceof ArrowEntity || entity instanceof SpectralArrowEntity) entity.discard();
            if (entity instanceof ServerPlayerEntity serverPlayer)
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PRESS_BUTTON, Color.RED);

            ci.cancel();
            return;
        }

        RedstoneOriginTracker redstoneTracker = WorldRegistry.get((ServerWorld) world)
                .getTrackerManager()
                .getRedstoneOriginTracker();

        redstoneTracker.record(pos, responsible);
    }

    @Unique
    private UUID getResponsiblePlayer(Entity entity, ServerWorld world) {
        if (entity instanceof ServerPlayerEntity player) return player.getUuid();
        if (entity instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();

            if (owner instanceof ServerPlayerEntity player) return player.getUuid();
            if (owner instanceof MobEntity mob) {
                InfluencedEntityTracker tracker = WorldRegistry.get(world)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                return tracker.getResponsible(mob);
            }
        }

        if (entity instanceof BoatEntity boat) {
            BoatOriginTracker tracker = WorldRegistry.get(world).getTrackerManager().getBoatOriginTracker();
            return tracker.getResponsiblePlayer(boat);
        }

        if (entity instanceof AbstractMinecartEntity minecart) {
            MinecartOriginTracker tracker = WorldRegistry.get(world).getTrackerManager().getMinecartOriginTracker();
            return tracker.getResponsiblePlayer(minecart);
        }

        if (entity instanceof MobEntity) {
            InfluencedEntityTracker tracker = WorldRegistry.get(world).getTrackerManager().getInfluencedEntityTracker();
            return tracker.getResponsible(entity);
        }

        return null;
    }
}