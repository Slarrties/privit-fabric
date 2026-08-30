package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.DamageResponsibilityChecker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameInteractMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void preventInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemFrameEntity self = (ItemFrameEntity) (Object) this;
        BlockPos pos = self.getBlockPos();

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUuid(), Rule.BUILD, pos, serverPlayer.getServerWorld())) {
            cir.setReturnValue(ActionResult.FAIL);
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemFrameEntity self = (ItemFrameEntity) (Object) this;
        if (self.getWorld().isClient()) return;

        ServerWorld serverWorld = (ServerWorld) self.getWorld();
        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverWorld);
        if (responsible == null) return;

        BlockPos pos = self.getBlockPos();
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, pos, serverWorld)) {
            cir.setReturnValue(false);

            if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            }
        }
    }
}