package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.UUID;

@AssociatedRule({Rule.CAUSE_EXPLOSIONS, Rule.USE_FIRE_STARTERS})
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorExplosionMixin {

    @Unique
    private static final ThreadLocal<UUID> LAST_ANCHOR_USER = new ThreadLocal<>(); // TODO

    @WrapOperation(
            method = "useWithoutItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/RespawnAnchorBlock;explode(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void rememberAnchorUserAndWrapExplosion(
            RespawnAnchorBlock instance,
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Operation<Void> original,
            @Local(argsOnly = true) Player player
    ) {
        UUID responsibleId = null;
        if (player instanceof ServerPlayer serverPlayer) {
            responsibleId = serverPlayer.getUUID();
            LAST_ANCHOR_USER.set(responsibleId);
        }

        original.call(instance, state, level, pos);

        if (responsibleId != null) {
            FireOriginTracker fireTracker = WorldRegistry.get(level)
                    .getTrackerManager()
                    .getFireOriginTracker();

            for (BlockPos firePos : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 4, 4))) {
                if (level.getBlockState(firePos).is(Blocks.FIRE)) {
                    fireTracker.record(firePos, responsibleId);
                }
            }
        }
        LAST_ANCHOR_USER.remove();
    }

    @WrapOperation(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
            )
    )
    private void wrapAnchorExplosion(
            ServerLevel level,
            Entity originalExploder,
            DamageSource damageSource,
            ExplosionDamageCalculator calculator,
            Vec3 explosionPos,
            float power,
            boolean createFire,
            Level.ExplosionInteraction interaction,
            Operation<Void> original
    ) {
        UUID responsibleId = LAST_ANCHOR_USER.get();
        Entity exploder = originalExploder;

        if (responsibleId != null) {
            ServerPlayer online = level.getServer()
                    .getPlayerList()
                    .getPlayer(responsibleId);
            if (online != null) {
                exploder = online;
            }
        }

        original.call(level, exploder, damageSource, calculator, explosionPos, power, createFire, interaction);
    }
}