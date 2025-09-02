package net.dragonmounts.plus.compat.registry;

import com.mojang.serialization.MapCodec;
import net.dragonmounts.plus.common.api.ArmorEffectSource;
import net.dragonmounts.plus.common.component.ListBasedArmorEffectSource;
import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.core.MappedRegistry;

public record ArmorEffectSourceType<T extends ArmorEffectSource>(MapCodec<T> codec) {
    public static final MappedRegistry<ArmorEffectSourceType<?>> REGISTRY = Dummy.get();
    public static final ArmorEffectSourceType<ListBasedArmorEffectSource> COMPONENT = Dummy.get();
    public static final ArmorEffectSourceType<ArmorEffectSource> BUILTIN = Dummy.get();
}