package dev.slarrties.privit.server.region.protection.mixin.cause_block_fall;

import java.util.UUID;

import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.tracking.context.BlockFallContext;
import dev.slarrties.privit.server.tracking.protection.BlockFallOriginTracker;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AssociatedRule(Rule.CAUSE_BLOCK_FALL)
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockProtectionMixin {

    @Shadow
    public boolean dropItem;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void discardUnauthorizedFall(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.getWorld().isClient()) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        if (self.isRemoved()) return;

        InfluencedEntityTracker influenced = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        UUID responsible = influenced.getResponsible(self);
        if (responsible == null) return;

        BlockPos pos = self.getBlockPos();
        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_BLOCK_FALL, pos, serverWorld);
        if (!allowed) {
            ServerPlayerEntity player = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(responsible);
            PlayerNotification.trySend(player, NotificationType.DENY_PLACE_BLOCK, Color.RED);
            this.dropItem = false;
            self.discard();
            ci.cancel();
        }
    }

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void preventUnauthorizedFallDamage(float fallDistance, float damageMultiplier,
                                               DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.getWorld().isClient()) return;
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;

        InfluencedEntityTracker influenced = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        UUID responsible = influenced.getResponsible(self);
        if (responsible == null) return;

        BlockPos pos = self.getBlockPos();
        boolean allowed = RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_BLOCK_FALL, pos, serverWorld);

        if (!allowed) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "spawnFromBlock", at = @At("RETURN"))
    private static void recordFallOrigin(World world, BlockPos pos, BlockState state,
                                         CallbackInfoReturnable<FallingBlockEntity> cir) {
        if (world.isClient()) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        FallingBlockEntity spawned = cir.getReturnValue();
        if (spawned == null) return;

        UUID responsible = resolveResponsible(serverWorld, pos);
        if (responsible == null) return;

        BlockFallOriginTracker blockFallTracker = WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getBlockFallOriginTracker();
        blockFallTracker.record(pos, responsible);

        if (!RegionPermissionChecker.isAllowed(responsible, Rule.CAUSE_BLOCK_FALL, pos, serverWorld)) {
            return;
        }

        WorldRegistry.get(serverWorld)
                .getTrackerManager()
                .getInfluencedEntityTracker()
                .record(spawned, responsible);
    }

    @Unique
    private static UUID resolveResponsible(ServerWorld world, BlockPos pos) {
        BlockFallContext ctx = BlockFallContext.getCurrent();
        if (ctx != null && ctx.getResponsible() != null) {
            return ctx.getResponsible();
        }

        return WorldRegistry.get(world)
                .getTrackerManager()
                .getBlockFallOriginTracker()
                .getResponsible(pos);
    }
}