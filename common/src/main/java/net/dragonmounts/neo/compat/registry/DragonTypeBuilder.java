package net.dragonmounts.neo.compat.registry;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import net.dragonmounts.neo.common.util.ArmorMaterialBuilder;
import net.dragonmounts.neo.common.util.ItemTierBuilder;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public final class DragonTypeBuilder {
    public static final ResourceLocation BONUS_ID = makeId("dragon_type_bonus");
    public final int color;
    public final ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> attributes = ImmutableMultimap.builder();
    public final ImmutableSet.Builder<ResourceKey<DamageType>> immunities = ImmutableSet.builder();
    public final ImmutableSet.Builder<Block> blocks = ImmutableSet.builder();
    public final ImmutableSet.Builder<ResourceKey<Biome>> biomes = ImmutableSet.builder();
    public final @Nullable ItemTierBuilder tier;
    public final @Nullable ArmorMaterialBuilder material;
    public boolean convertible = true;
    public @NotNull ParticleOptions sneezeParticle = ParticleTypes.LARGE_SMOKE;
    public @NotNull ParticleOptions eggParticle = ParticleTypes.MYCELIUM;
    public @NotNull MapColor scaleColor = MapColor.NONE;
    public TagKey<Item> scales;

    public DragonTypeBuilder(int color, @Nullable ArmorMaterialBuilder material, @Nullable ItemTierBuilder tier) {
        this.color = color;
        this.tier = tier;
        this.material = material;
    }

    public DragonTypeBuilder notConvertible() {
        return Dummy.get();
    }

    public DragonTypeBuilder putAttributeModifier(Holder<Attribute> attribute, ResourceLocation identifier, double value, AttributeModifier.Operation operation) {
        return Dummy.get();
    }

    public DragonTypeBuilder addImmunity(ResourceKey<DamageType> type) {
        return Dummy.get();
    }

    public DragonTypeBuilder addHabitat(Block block) {
        return Dummy.get();
    }

    public DragonTypeBuilder addHabitat(ResourceKey<Biome> biome) {
        return Dummy.get();
    }

    public DragonTypeBuilder setSneezeParticle(SimpleParticleType particle) {
        return Dummy.get();
    }

    public DragonTypeBuilder setEggParticle(SimpleParticleType particle) {
        return Dummy.get();
    }

    public DragonTypeBuilder setMaterial(TagKey<Item> material) {
        return Dummy.get();
    }

    public DragonTypeBuilder setScaleColor(MapColor color) {
        return Dummy.get();
    }

    public <T extends DragonType> T register(
            BiFunction<ResourceLocation, DragonTypeBuilder, T> factory,
            ResourceLocation identifier
    ) {
        return Dummy.get();
    }
}
