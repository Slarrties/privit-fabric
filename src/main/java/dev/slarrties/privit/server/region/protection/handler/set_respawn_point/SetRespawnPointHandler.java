package dev.slarrties.privit.server.region.protection.handler.set_respawn_point;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.SET_RESPAWN_POINT)
public final class SetRespawnPointHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.SET_RESPAWN_POINT; }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof BedBlock) {
                if (level.dimension() != Level.OVERWORLD) {
                    return InteractionResult.PASS;
                }
            }

            if (!isRespawnSettingBlock(state, level)) return InteractionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.SET_RESPAWN_POINT, pos);
            if (allowed) return InteractionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_SET_RESPAWN_POINT, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);
            return InteractionResult.FAIL;
        });
    }

    private static boolean isRespawnSettingBlock(BlockState state, Level level) {
        if (state.getBlock() instanceof BedBlock) return level.dimension() == Level.OVERWORLD;

        return state.getBlock() instanceof RespawnAnchorBlock;
    }
}