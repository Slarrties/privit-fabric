package dev.slarrties.privit.server.region.util;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.region.protection.AssociatedRule;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

@AssociatedRule(Rule.BUILD)
public final class DamageResponsibilityChecker {

    private DamageResponsibilityChecker() {}

    public static UUID getResponsibleAttacker(DamageSource source, ServerLevel level) {
        Entity attacker = source.getDirectEntity();
        InfluencedEntityTracker tracker = WorldRegistry.get(level)
                .getTrackerManager()
                .getInfluencedEntityTracker();

        if (attacker == null) return null;
        if (attacker instanceof ServerPlayer player) return player.getUUID();
        if (attacker instanceof Mob mob) return tracker.getResponsible(mob);
        if (attacker instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();

            if (owner instanceof ServerPlayer player) return player.getUUID();
            if (owner instanceof Mob mob) return tracker.getResponsible(mob);
        }

        return null;
    }
}