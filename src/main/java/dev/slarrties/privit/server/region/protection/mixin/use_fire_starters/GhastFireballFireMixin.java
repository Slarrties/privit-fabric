package dev.slarrties.privit.server.region.protection.mixin.use_fire_starters;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.ExplosionOriginTracker;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.projectile.FireballEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@AssociatedRule(Rule.USE_FIRE_STARTERS)
@Mixin(FireballEntity.class)
public abstract class GhastFireballFireMixin {

    @Inject(method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V", at = @At("TAIL"))
    private void trackFireAfterGhastFireballExplosion(CallbackInfo ci) {
        FireballEntity fireball = (FireballEntity) (Object) this;

        if (fireball.getWorld() instanceof ServerWorld serverWorld) {
            ExplosionOriginTracker explosionOriginTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getExplosionOriginTracker();
            FireOriginTracker fireOriginTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getFireOriginTracker();

            UUID responsible = explosionOriginTracker.getResponsiblePlayer(fireball);
            if (responsible == null) return;

            BlockPos center = BlockPos.ofFloored(fireball.getPos());

            for (BlockPos pos : BlockPos.iterateOutwards(center, 3, 3, 3)) {
                if (serverWorld.getBlockState(pos).isOf(Blocks.FIRE) || serverWorld.getBlockState(pos).isOf(Blocks.SOUL_FIRE)) {
                    fireOriginTracker.record(pos, responsible);
                }
            }
        }
    }
}