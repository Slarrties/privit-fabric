package dev.slarrties.privit.server.region.protection.mixin.interact_with_doors;

import net.minecraft.entity.ai.goal.DoorInteractGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {
    @Accessor("mob")
    MobEntity getMob();

    @Accessor("doorPos")
    BlockPos getDoorPos();
}