package net.dragonmounts.neo.common.api;

import net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.minecraft.server.level.ServerPlayer;

/// @see net.minecraft.advancements.critereon.BredAnimalsTrigger
public interface BredDragonsTrigger {
    default void neodragonmounts$trigger(
            ServerPlayer player,
            ServerDragonEntity parent,
            ServerDragonEntity partner,
            HatchableDragonEggEntity egg
    ) {}
}
