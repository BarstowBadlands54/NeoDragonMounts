package net.dragonmounts.neo.common.api;

import net.dragonmounts.neo.compat.registry.DragonType;

public interface DragonTypified {
    DragonType getDragonType();

    interface Mutable extends DragonTypified {
        void setDragonType(DragonType type, boolean reset);
    }
}