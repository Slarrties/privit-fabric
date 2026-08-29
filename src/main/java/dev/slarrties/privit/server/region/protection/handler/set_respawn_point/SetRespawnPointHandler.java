package dev.slarrties.privit.server.region.protection.handler.set_respawn_point;

import dev.slarrties.privit.common.region.Color;
import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.common.notification.NotificationType;
import dev.slarrties.privit.server.util.PlayerNotification;
import dev.slarrties.privit.server.region.util.InventorySyncSystem;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;
import dev.slarrties.privit.server.region.protection.RegionPermissionChecker;

import net.minecraft.world.World;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

@AssociatedRule(Rule.SET_RESPAWN_POINT)
public final class SetRespawnPointHandler implements RuleEventHandler {

    @Override
    public Rule getRule() { return Rule.SET_RESPAWN_POINT; }

    @Override
    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof BedBlock) {
                if (world.getRegistryKey() != World.OVERWORLD) {
                    return ActionResult.PASS;
                }
            }

            if (!isRespawnSettingBlock(state, world)) return ActionResult.PASS;

            boolean allowed = RegionPermissionChecker.isAllowed(serverPlayer, Rule.SET_RESPAWN_POINT, pos);
            if (allowed) return ActionResult.PASS;

            PlayerNotification.trySend(serverPlayer, NotificationType.DENY_SET_RESPAWN_POINT, Color.RED);
            InventorySyncSystem.syncHandSlot(serverPlayer, hand);

            return ActionResult.FAIL;
        });
    }

    private static boolean isRespawnSettingBlock(BlockState state, World world) {
        if (state.getBlock() instanceof BedBlock) {
            return world.getRegistryKey() == World.OVERWORLD;
        }

        return state.getBlock() instanceof RespawnAnchorBlock;
    }
}