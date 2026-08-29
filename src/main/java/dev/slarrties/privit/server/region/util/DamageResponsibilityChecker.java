package dev.slarrties.privit.server.region.util;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
public final class DamageResponsibilityChecker {

    private DamageResponsibilityChecker() {}

    public static UUID getResponsibleAttacker(DamageSource source, ServerWorld world) {
        Entity attacker = source.getAttacker();
        InfluencedEntityTracker tracker = WorldRegistry.get(world)
                .getTrackerManager()
                .getInfluencedEntityTracker();
        if (attacker == null) return null;
        if (attacker instanceof ServerPlayerEntity player) return player.getUuid();
        if (attacker instanceof MobEntity) return tracker.getResponsible(attacker);
        if (attacker instanceof ProjectileEntity projectile) {
            Entity owner = projectile.getOwner();

            if (owner instanceof ServerPlayerEntity player) return player.getUuid();
            if (owner instanceof MobEntity) return tracker.getResponsible(owner);
        }

        return null;
    }
}