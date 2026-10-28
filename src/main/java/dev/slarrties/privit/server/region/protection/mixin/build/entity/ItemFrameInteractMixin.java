package dev.slarrties.privit.server.region.protection.mixin.build.entity;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.DamageResponsibilityChecker;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
@Mixin(ItemFrame.class)
public abstract class ItemFrameInteractMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void preventInteract(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ItemFrame self = (ItemFrame) (Object) this;
        BlockPos pos = self.blockPosition();

        if (!RegionPermissionChecker.isAllowed(serverPlayer.getUUID(), Rule.BUILD, pos, serverPlayer.level())) {
            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventDamage(ServerLevel serverLevel, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        ItemFrame self = (ItemFrame) (Object) this;
        UUID responsible = DamageResponsibilityChecker.getResponsibleAttacker(source, serverLevel);
        if (responsible == null) return;

        BlockPos pos = self.blockPosition();
        if (!RegionPermissionChecker.isAllowed(responsible, Rule.BUILD, pos, serverLevel)) {
            if (source.getEntity() instanceof ServerPlayer serverPlayer) {
                PlayerNotification.trySend(serverPlayer, NotificationType.DENY_BREAK_BLOCK, Color.RED);
            }
            cir.setReturnValue(false);
        }
    }
}