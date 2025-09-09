package net.dragonmounts.neo.compat.registry;

import net.dragonmounts.neo.common.capability.ArmorEffectManager;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.MappedRegistry;
import net.minecraft.world.entity.player.Player;

public interface ArmorEffect {
    MappedRegistry<ArmorEffect> REGISTRY = Dummy.get();

    boolean activate(ArmorEffectManager manager, Player player, int level);
}
