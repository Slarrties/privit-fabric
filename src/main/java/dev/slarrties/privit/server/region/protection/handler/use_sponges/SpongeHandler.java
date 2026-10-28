package dev.slarrties.privit.server.region.protection.handler.use_sponges;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.tracking.context.SpongePlacementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.USE_SPONGES)
public final class SpongeHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.USE_SPONGES; }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(player.getItemInHand(hand).getItem() instanceof BlockItem blockItem)) return InteractionResult.PASS;
            if (!(blockItem.getBlock() instanceof SpongeBlock)) return InteractionResult.PASS;

            BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getDirection().getUnitVec3i());
            SpongePlacementContext.push(serverPlayer, placePos);
            return InteractionResult.PASS;
        });
    }
}