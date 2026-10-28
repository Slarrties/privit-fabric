package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.UUID;

@AssociatedRule({Rule.CAUSE_EXPLOSIONS, Rule.USE_FIRE_STARTERS})
@Mixin(BedBlock.class)
public abstract class BedExplosionMixin {

    @WrapOperation(
            method = "useWithoutItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            )
    )
    private void wrapBedExplosion(
            Level level,
            Entity originalExploder,
            DamageSource damageSource,
            ExplosionDamageCalculator calculator,
            Vec3 explosionPos,
            float power,
            boolean createFire,
            Level.ExplosionInteraction interaction,
            Operation<Void> original,
            @Local(argsOnly = true) Player player
    ) {
        Entity exploder = originalExploder;
        UUID responsibleId = null;

        if (player instanceof ServerPlayer serverPlayer) {
            exploder = serverPlayer;
            responsibleId = serverPlayer.getUUID();
        }

        original.call(level, exploder, damageSource, calculator, explosionPos, power, createFire, interaction);

        if (responsibleId != null && createFire && level instanceof ServerLevel serverLevel) {
            FireOriginTracker fireTracker = WorldRegistry.get(serverLevel)
                    .getTrackerManager()
                    .getFireOriginTracker();
            BlockPos center = BlockPos.containing(explosionPos);

            for (BlockPos firePos : BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))) {
                if (level.getBlockState(firePos).is(Blocks.FIRE)) {
                    fireTracker.record(firePos, responsibleId);
                }
            }
        }
    }
}