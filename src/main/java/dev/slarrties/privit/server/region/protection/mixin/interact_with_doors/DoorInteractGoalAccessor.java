package dev.slarrties.privit.server.region.protection.mixin.interact_with_doors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {

    @Accessor("mob")
    Mob getMob();

    @Accessor("doorPos")
    BlockPos getDoorPos();
}