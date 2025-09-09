package net.dragonmounts.neo.common.component;

import com.mojang.serialization.MapCodec;
import net.dragonmounts.neo.common.api.ArmorEffectSource;
import net.dragonmounts.neo.common.capability.ArmorEffectManager;
import net.dragonmounts.neo.common.component.impl.ArmorEffectEntry;
import net.dragonmounts.neo.compat.registry.ArmorEffectSourceType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public interface ListBasedArmorEffectSource extends ArmorEffectSource {
    List<ArmorEffectEntry> getEffects();

    static ListBasedArmorEffectSource of(ArmorEffectEntry... effects) {
        return of(List.of(effects));
    }

    static ListBasedArmorEffectSource of(List<ArmorEffectEntry> effects) {
        return effects.isEmpty() ? empty() : () -> effects;
    }

    static ListBasedArmorEffectSource empty() {
        return Collections::emptyList;
    }

    MapCodec<ListBasedArmorEffectSource> CODEC = ArmorEffectEntry.CODEC.listOf().xmap(
            ListBasedArmorEffectSource::of,
            ListBasedArmorEffectSource::getEffects
    ).fieldOf("effects");

    @Override
    default void affect(ArmorEffectManager manager, Player player, ItemStack stack) {
        for (var entry : this.getEffects()) {
            manager.addLevel(entry.effect(), entry.level());
        }
    }

    @Override
    default ArmorEffectSourceType<?> getType() {
        return ArmorEffectSourceType.COMPONENT;
    }
}
