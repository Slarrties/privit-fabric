package dev.slarrties.privit.common.util;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public interface PlayerNameProvider {

    PlayerIdentity getIdentity(UUID uuid);

    default List<PlayerIdentity> getIdentities(Iterable<UUID> uuids) {
        List<PlayerIdentity> result = new ArrayList<>();

        for (UUID uuid : uuids)
            result.add(getIdentity(uuid));

        return result;
    }

    void updateName(UUID uuid, String name);
}