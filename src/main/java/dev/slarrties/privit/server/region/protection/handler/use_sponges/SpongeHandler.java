package dev.slarrties.privit.server.region.protection.handler.use_sponges;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.tracking.context.SpongePlacementContext;

import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.SpongeBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.USE_SPONGES)
public final class SpongeHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return Rule.USE_SPONGES;
    }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!(player.getStackInHand(hand).getItem() instanceof BlockItem blockItem)) return ActionResult.PASS;
            if (!(blockItem.getBlock() instanceof SpongeBlock)) return ActionResult.PASS;

            BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
            SpongePlacementContext.push(serverPlayer, placePos);

            return ActionResult.PASS;
        });
    }
}