package net.dragonmounts.neo.compat.registry;

import com.mojang.serialization.MapCodec;
import net.dragonmounts.neo.common.api.ArmorEffectSource;
import net.dragonmounts.neo.common.component.ListBasedArmorEffectSource;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryBuilder;

import static net.dragonmounts.neo.common.DragonMountsShared.ARMOR_EFFECT_SOURCE;
import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;

public record ArmorEffectSourceType<T extends ArmorEffectSource>(MapCodec<T> codec) {
    public static final ResourceLocation DEFAULT = withDefaultNamespace("component");
    public static final Registry<net.dragonmounts.neo.compat.registry.ArmorEffectSourceType<?>> REGISTRY
            = new RegistryBuilder<>(ARMOR_EFFECT_SOURCE).sync(true).defaultKey(DEFAULT).create();
    public static final net.dragonmounts.neo.compat.registry.ArmorEffectSourceType<ListBasedArmorEffectSource> COMPONENT =
            new net.dragonmounts.neo.compat.registry.ArmorEffectSourceType<>(ListBasedArmorEffectSource.CODEC);
    public static final net.dragonmounts.neo.compat.registry.ArmorEffectSourceType<ArmorEffectSource> BUILTIN =
            new net.dragonmounts.neo.compat.registry.ArmorEffectSourceType<>(MapCodec.unit(ListBasedArmorEffectSource::empty));
}