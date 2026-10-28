package dev.slarrties.privit.server.region.protection.mixin.create_nether_portals;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.NetherPortalEntryTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@AssociatedRule(Rule.CREATE_NETHER_PORTALS)
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalCollisionMixin {

    @Inject(method = "entityInside", at = @At("HEAD"))
    private void recordPlayerPortalEntry(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise, CallbackInfo ci) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        NetherPortalEntryTracker.recordEntry(player);
    }
}