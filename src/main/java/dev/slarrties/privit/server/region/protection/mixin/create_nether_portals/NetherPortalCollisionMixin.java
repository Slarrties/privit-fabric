package dev.slarrties.privit.server.region.protection.mixin.create_nether_portals;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.NetherPortalEntryTracker;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.CREATE_NETHER_PORTALS)
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalCollisionMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"))
    private void recordPlayerPortalEntry(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;

        NetherPortalEntryTracker.recordEntry(player);
    }
}
