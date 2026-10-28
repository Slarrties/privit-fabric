package dev.slarrties.privit.server.region.protection.mixin.cause_block_fall;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule(Rule.CAUSE_BLOCK_FALL)
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockProtectionMixin {

    @Shadow
    public boolean dropItem;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void discardUnauthorizedFall(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;
        if (self.isRemoved()) return;

        InfluencedEntityTracker influenced = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        UUID responsible = influenced.getResponsible(self);
        if (responsible == null) return;

        BlockPos pos = self.blockPosition();
        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_BLOCK_FALL, pos, serverLevel);
        if (!allowed) {
            ServerPlayer player = serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(responsible);
            PlayerNotification.trySend(player, NotificationType.DENY_PLACE_BLOCK, Color.RED);
            this.dropItem = false;
            self.discard();
            ci.cancel();
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void preventUnauthorizedFallDamage(double fallDistance, float damageModifier, DamageSource damageSource,
                                               CallbackInfoReturnable<Boolean> cir) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        InfluencedEntityTracker influenced = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        UUID responsible = influenced.getResponsible(self);
        if (responsible == null) return;

        BlockPos pos = self.blockPosition();
        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_BLOCK_FALL, pos, serverLevel);
        if (!allowed) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "fall", at = @At("RETURN"))
    private static void recordFallOrigin(Level level, BlockPos pos, BlockState state,
                                         CallbackInfoReturnable<FallingBlockEntity> cir) {
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        FallingBlockEntity spawned = cir.getReturnValue();
        if (spawned == null) return;

        UUID responsible = resolveResponsible(serverLevel, pos);
        if (responsible == null) return;

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, responsible);

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_BLOCK_FALL, pos, serverLevel)) {
            return;
        }

        WorldRegistry.get(serverLevel)
                .getTrackerManager()
                .getInfluencedEntityTracker()
                .record(spawned, responsible);
    }

    @Unique
    private static UUID resolveResponsible(ServerLevel level, BlockPos pos) {
        BlockFallContext ctx = BlockFallContext.getCurrent();
        if (ctx != null && ctx.getResponsible() != null) {
            return ctx.getResponsible();
        }

        return WorldRegistry.get(level)
                .getTrackerManager()
                .getBlockFallOriginTracker()
                .getResponsible(pos);
    }
}