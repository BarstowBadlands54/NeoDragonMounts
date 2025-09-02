package net.dragonmounts.plus.compat.registry;

import net.dragonmounts.plus.common.capability.ArmorEffectManager;
import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.core.MappedRegistry;
import net.minecraft.world.entity.player.Player;

public interface ArmorEffect {
    MappedRegistry<ArmorEffect> REGISTRY = Dummy.get();

    boolean activate(ArmorEffectManager manager, Player player, int level);
}
