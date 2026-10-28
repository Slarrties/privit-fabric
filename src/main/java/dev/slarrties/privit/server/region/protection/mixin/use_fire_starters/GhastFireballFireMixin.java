package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(LargeFireball.class)
public abstract class GhastFireballFireMixin {

    @Inject(method = "onHit", at = @At("TAIL"))
    private void trackFireAfterGhastFireballExplosion(net.minecraft.world.phys.HitResult hitResult, CallbackInfo ci) {
        LargeFireball fireball = (LargeFireball) (Object) this;
        if (fireball.level() instanceof ServerLevel serverLevel) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getExplosionOriginTracker();
            FireOriginTracker fireOriginTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getFireOriginTracker();
            UUID responsible = explosionOriginTracker.getResponsible(fireball);
            if (responsible == null) return;

            BlockPos center = BlockPos.containing(fireball.position());
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
                if (serverLevel.getBlockState(pos).is(Blocks.FIRE) || serverLevel.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
                    fireOriginTracker.record(pos, responsible);
                }
            }
        }
    }
}