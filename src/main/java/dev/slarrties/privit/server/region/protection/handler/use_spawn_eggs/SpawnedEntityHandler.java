package dev.slarrties.privit.server.region.protection.handler.use_spawn_eggs;

import dev.slarrties.privit.common.region.rule.Rule;
import dev.slarrties.privit.server.world.WorldRegistry;
import dev.slarrties.privit.server.tracking.context.EntitySpawnContext;
import dev.slarrties.privit.server.tracking.protection.InfluencedEntityTracker;
import dev.slarrties.privit.server.region.protection.handler.RuleEventHandler;

import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

// TODO: move it to a separate util component?
public class SpawnedEntityHandler implements RuleEventHandler {

    @Override
    public Rule getRule() {
        return null;
    }

    @Override
    public void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.isClient) return;

            EntitySpawnContext ctx = EntitySpawnContext.getCurrent();
            if (ctx == null) return;
            if (ctx.isNearSpawnPosition(entity)) {
                InfluencedEntityTracker tracker = WorldRegistry.get((ServerWorld) world)
                        .getTrackerManager()
                        .getInfluencedEntityTracker();
                tracker.record(entity, ctx.getPlayer().getUuid());
            }

            EntitySpawnContext.pop();
        });
    }
}