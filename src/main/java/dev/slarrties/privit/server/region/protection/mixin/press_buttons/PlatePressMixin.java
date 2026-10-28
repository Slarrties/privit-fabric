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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.PRESS_BUTTONS)
@Mixin(BasePressurePlateBlock.class)
public abstract class PlatePressMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void preventPlateActivation(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise, CallbackInfo ci) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        UUID responsible = getResponsiblePlayer(entity, serverLevel);
        if (responsible == null) return;

        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.PRESS_BUTTONS, pos, serverLevel);
        if (!allowed) {
            if (entity instanceof Arrow || entity instanceof SpectralArrow) entity.discard();
            if (entity instanceof ServerPlayer serverPlayer)
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PRESS_BUTTON, Color.RED);
            ci.cancel();
            return;
        }

        RedstoneOriginTracker redstoneTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getRedstoneOriginTracker();
        redstoneTracker.record(pos, responsible);
    }

    @Unique
    private UUID getResponsiblePlayer(Entity entity, ServerLevel level) {
        if (entity instanceof ServerPlayer player) return player.getUUID();
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer player) return player.getUUID();
            if (owner instanceof Mob mob) {
                InfluencedEntityTracker tracker = WorldRegistry.get(level)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                return tracker.getResponsible(mob);
            }
        }

        if (entity instanceof Boat boat) {
            BoatOriginTracker tracker = WorldRegistry.get(level).getTrackerManager().getBoatOriginTracker();
            return tracker.getResponsible(boat);
        }

        if (entity instanceof AbstractMinecart minecart) {
            MinecartOriginTracker tracker = WorldRegistry.get(level).getTrackerManager().getMinecartOriginTracker();
            return tracker.getResponsible(minecart);
        }

        if (entity instanceof Mob mob) {
            InfluencedEntityTracker tracker = WorldRegistry.get(level).getTrackerManager().getInfluencedEntityTracker();
            return tracker.getResponsible(mob);
        }

        return null;
    }
}