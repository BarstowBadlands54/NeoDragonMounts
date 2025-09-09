package net.dragonmounts.neo.compat.registry;

import com.mojang.serialization.MapCodec;
import net.dragonmounts.neo.common.api.ArmorEffectSource;
import net.dragonmounts.neo.common.component.ListBasedArmorEffectSource;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.MappedRegistry;

public record ArmorEffectSourceType<T extends ArmorEffectSource>(MapCodec<T> codec) {
    public static final MappedRegistry<ArmorEffectSourceType<?>> REGISTRY = Dummy.get();
    public static final ArmorEffectSourceType<ListBasedArmorEffectSource> COMPONENT = Dummy.get();
    public static final ArmorEffectSourceType<ArmorEffectSource> BUILTIN = Dummy.get();
}