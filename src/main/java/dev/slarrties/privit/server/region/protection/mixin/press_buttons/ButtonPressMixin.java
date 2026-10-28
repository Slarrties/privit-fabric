package dev.slarrties.privit.server.region.protection.mixin.press_buttons;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.PRESS_BUTTONS)
@Mixin(ButtonBlock.class)
public abstract class ButtonPressMixin {

    @Shadow @Final
    private BlockSetType type;

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void preventButtonPress(BlockState state, Level level, BlockPos pos, Player player,
                                    BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.PRESS_BUTTONS, pos, serverPlayer.level())) return;

        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PRESS_BUTTON, Color.RED);
        serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = "checkPressed", at = @At("HEAD"), cancellable = true)
    private void preventUnauthorizedProjectileActivation(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (state.getValue(ButtonBlock.POWERED)) return;
        if (!this.type.canButtonBeActivatedByArrows()) return;

        AbstractArrow projectile = level.getEntitiesOfClass(
                AbstractArrow.class,
                state.getShape(level, pos).bounds().move(pos)
        ).stream().findFirst().orElse(null);

        if (projectile == null) return;

        UUID responsible = getResponsiblePlayer(projectile, level);
        if (responsible == null) return;

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.PRESS_BUTTONS, pos, serverLevel)) {
            if (projectile instanceof Arrow || projectile instanceof SpectralArrow) {
                projectile.discard();
            }
            if (projectile.getOwner() instanceof ServerPlayer serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PRESS_BUTTON, Color.RED);
            }
            ci.cancel();
        }
    }

    @Unique
    private UUID getResponsiblePlayer(Entity entity, Level level) {
        if (entity instanceof ServerPlayer player) return player.getUUID();
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof ServerPlayer player)
                return player.getUUID();
            if (owner instanceof Mob mob && level instanceof ServerLevel serverLevel) {
                InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                return tracker.getResponsible(mob);
            }
        }
        if (entity instanceof Mob mob && level instanceof ServerLevel serverLevel) {
            InfluencedEntityTracker tracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            return tracker.getResponsible(mob);
        }
        return null;
    }
}