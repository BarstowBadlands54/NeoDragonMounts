package net.dragonmounts.neo.compat.registry;

import com.mojang.serialization.MapCodec;
import net.dragonmounts.neo.common.api.ArmorEffectSource;
import net.dragonmounts.neo.common.component.ListBasedArmorEffectSource;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;

import static net.dragonmounts.neo.common.DragonMountsShared.ARMOR_EFFECT_SOURCE;
import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
import static net.dragonmounts.neo.compat.registry.RegistryHandler.makeDefaultedRegistry;
import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;

public record ArmorEffectSourceType<T extends ArmorEffectSource>(MapCodec<T> codec) {
    public static final MappedRegistry<ArmorEffectSourceType<?>> REGISTRY;
    public static final ArmorEffectSourceType<ListBasedArmorEffectSource> COMPONENT =
            new ArmorEffectSourceType<>(ListBasedArmorEffectSource.CODEC);
    public static final ArmorEffectSourceType<ArmorEffectSource> BUILTIN =
            new ArmorEffectSourceType<>(MapCodec.unit(ListBasedArmorEffectSource::empty));

    static {
        var key = withDefaultNamespace("component");
        REGISTRY = makeDefaultedRegistry(ARMOR_EFFECT_SOURCE, key);
        Registry.register(REGISTRY, key, COMPONENT);
        Registry.register(REGISTRY, makeId("builtin"), BUILTIN);
    }
}