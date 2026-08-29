package dev.slarrties.privit.server.region.protection.mixin.cause_explosions;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.FireOriginTracker;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@AssociatedRule({Rule.CAUSE_EXPLOSIONS, Rule.USE_FIRE_STARTERS})
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorExplosionMixin {

    @Unique
    private static final ThreadLocal<UUID> LAST_ANCHOR_USER = new ThreadLocal<>();

    @Inject(
            method = "onUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/RespawnAnchorBlock;explode(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"
            )
    )
    private void rememberAnchorUser(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            LAST_ANCHOR_USER.set(serverPlayer.getUuid());
        }
    }

    @WrapOperation(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;Lnet/minecraft/util/math/Vec3d;FZLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"
            )
    )
    private Explosion wrapAnchorExplosion(
            World world,
            Entity originalExploder,
            DamageSource damageSource,
            ExplosionBehavior behavior,
            Vec3d explosionPos,
            float power,
            boolean createFire,
            World.ExplosionSourceType explosionSourceType,
            Operation<Explosion> original
    ) {
        UUID responsibleId = LAST_ANCHOR_USER.get();
        LAST_ANCHOR_USER.remove();
        Entity exploder = originalExploder;

        if (responsibleId != null && world instanceof ServerWorld serverWorld) {
            ServerPlayerEntity online = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(responsibleId);
            if (online != null) {
                exploder = online;
            }
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