package dev.slarrties.privit.server.tracking.context;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface SculkCursorDuck {

    @Nullable
    UUID getResponsible();

    void setResponsible(@Nullable UUID responsible);
}