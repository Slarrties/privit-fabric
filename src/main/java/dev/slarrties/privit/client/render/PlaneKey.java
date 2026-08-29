package dev.slarrties.privit.client.render;

import net.minecraft.util.math.Direction;
import java.util.Objects;

record PlaneKey(Direction direction, double fixedCoord) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaneKey that)) return false;
        return Double.compare(fixedCoord, that.fixedCoord) == 0 && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, fixedCoord);
    }
}