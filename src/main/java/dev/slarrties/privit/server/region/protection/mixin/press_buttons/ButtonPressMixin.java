package dev.slarrties.privit.server.region.protection.mixin.press_buttons;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.BlockSetType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

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

    @Shadow @Final private BlockSetType blockSetType;

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void preventButtonPress(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                    BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (RegionPermissionChecker.isAllowed(serverPlayer, Rule.PRESS_BUTTONS, pos)) return;

        cir.setReturnValue(ActionResult.FAIL);
        serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
        PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PRESS_BUTTON, Color.RED);
    }

    @Inject(method = "tryPowerWithProjectiles", at = @At("HEAD"), cancellable = true)
    private void preventUnauthorizedProjectileActivation(BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (world.isClient || (Boolean) state.get(ButtonBlock.POWERED)) return;
        if (!this.blockSetType.canButtonBeActivatedByArrows()) return;

        // TODO: if there is already a projectile in the button area, the new one will not be added.
        PersistentProjectileEntity projectile = world.getNonSpectatingEntities(
                PersistentProjectileEntity.class,
                state.getOutlineShape(world, pos).getBoundingBox().offset(pos)
        ).stream().findFirst().orElse(null);
        if (projectile == null) return;

        UUID responsible = getResponsiblePlayer(projectile, (ServerWorld) world);
        if (responsible == null) return;

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.PRESS_BUTTONS, pos, (ServerWorld) world)) {
            if (projectile instanceof ArrowEntity || projectile instanceof SpectralArrowEntity)
                projectile.discard();
            if (projectile.getOwner() instanceof ServerPlayerEntity serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_PRESS_BUTTON, Color.RED);
            }

            ci.cancel();
        }
    }

    @Unique
    private UUID getResponsiblePlayer(Entity entity, ServerWorld world) {
        if (entity instanceof ServerPlayerEntity player) return player.getUuid();

        if (entity instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();

            if (owner instanceof ServerPlayerEntity player)
                return player.getUuid();
            if (owner instanceof MobEntity mob) {
                InfluencedEntityTracker tracker = WorldRegistry.get(world)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                return tracker.getResponsible(mob);
            }
        }

        if (entity instanceof MobEntity) {
            InfluencedEntityTracker tracker = WorldRegistry.get(world)
                    .getTrackerManager()
                    .getInfluencedEntityTracker();
            return tracker.getResponsible(entity);
        }

        return null;
    }
}