package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@AssociatedRule({Rule.CAUSE_EXPLOSIONS, Rule.USE_FIRE_STARTERS})
@Mixin(BedBlock.class)
public abstract class BedExplosionMixin {

    @WrapOperation(
            method = "onUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;Lnet/minecraft/util/math/Vec3d;FZLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"
            )
    )
    private Explosion wrapBedExplosion(
            World world,
            Entity originalExploder,
            DamageSource damageSource,
            ExplosionBehavior behavior,
            Vec3d explosionPos,
            float power,
            boolean createFire,
            World.ExplosionSourceType explosionSourceType,
            Operation<Explosion> original,
            @Local(argsOnly = true) PlayerEntity player
    ) {
        Entity exploder = originalExploder;
        UUID responsibleId = null;

        if (player instanceof ServerPlayerEntity serverPlayer) {
            exploder = serverPlayer;
            responsibleId = serverPlayer.getUuid();
        }

        Explosion explosion = original.call(world, exploder, damageSource, behavior, explosionPos, power, createFire, explosionSourceType);

        if (responsibleId != null && createFire && world instanceof ServerWorld serverWorld) {
            FireOriginTracker fireTracker = WorldRegistry.get(serverWorld)
                    .getTrackerManager()
                    .getFireOriginTracker();
            BlockPos center = BlockPos.ofFloored(explosionPos);

            for (BlockPos firePos : BlockPos.iterateOutwards(center, 9, 9, 9)) {
                if (world.getBlockState(firePos).isOf(Blocks.FIRE)) {
                    fireTracker.record(firePos, responsibleId);
                }
            }
        }

        return explosion;
    }
}