package net.dragonmounts.neo.compat.registry;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import net.dragonmounts.neo.common.util.ArmorMaterialBuilder;
import net.dragonmounts.neo.common.util.ItemTierBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.BiFunction;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
//fabric
public final class DragonTypeBuilder {
    public static final ResourceLocation BONUS_ID = makeId("dragon_type_bonus");
    public final int color;
    public final ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> attributes = ImmutableMultimap.builder();
    public final ImmutableSet.Builder<ResourceKey<DamageType>> immunities = ImmutableSet.builder();
    public final ImmutableSet.Builder<Holder<MobEffect>> effectImmunities = ImmutableSet.builder();
    public final ImmutableSet.Builder<Block> blocks = ImmutableSet.builder();
    public final ImmutableSet.Builder<ResourceKey<Biome>> biomes = ImmutableSet.builder();
    public final @Nullable ItemTierBuilder tier;
    public final @Nullable ArmorMaterialBuilder material;
    public boolean convertible = true;
    public @NotNull ParticleOptions sneezeParticle = ParticleTypes.LARGE_SMOKE;
    public @NotNull ParticleOptions eggParticle = ParticleTypes.MYCELIUM;
    public @NotNull MapColor scaleColor = MapColor.NONE;
    public TagKey<Item> scales;
    public ResourceLocation geoModel = makeId("geo/model/dragonmounts2.dragon.normal.geo.json");
    public ResourceLocation headGeoModel = makeId("geo/head/dragonmounts2.head_block.base.geo.json");
    public ResourceLocation texture;
    public final EnumMap<ArmorItem.Type, ResourceLocation> armorGeo = new EnumMap<>(ArmorItem.Type.class);
    public @Nullable ResourceLocation armorTexture;


    public DragonTypeBuilder(int color, @Nullable ArmorMaterialBuilder material, @Nullable ItemTierBuilder tier) {
        this.color = color;
        this.tier = tier;
        this.material = material;
        // ignore suffocation damage
        this.addImmunity(DamageTypes.ON_FIRE).addImmunity(DamageTypes.IN_FIRE)
                .addImmunity(DamageTypes.HOT_FLOOR)
                .addImmunity(DamageTypes.LAVA)
                .addImmunity(DamageTypes.DROWN)
                .addImmunity(DamageTypes.IN_WALL)
                .addImmunity(DamageTypes.CACTUS) // assume that cactus needles don't do much damage to animals with horned scales
                .addImmunity(DamageTypes.DRAGON_BREATH); // ignore damage from vanilla ender dragon. I kinda disabled this because it wouldn't make any sense, feel free to re enable
    }

    public DragonTypeBuilder notConvertible() {
        this.convertible = false;
        return this;
    }

    public DragonTypeBuilder putAttributeModifier(Holder<Attribute> attribute, ResourceLocation identifier, double value, AttributeModifier.Operation operation) {
        this.attributes.put(attribute, new AttributeModifier(identifier, value, operation));
        return this;
    }

    public DragonTypeBuilder addImmunity(ResourceKey<DamageType> type) {
        this.immunities.add(type);
        return this;
    }

    public DragonTypeBuilder addHabitat(Block block) {
        this.blocks.add(block);
        return this;
    }

    public DragonTypeBuilder addHabitat(ResourceKey<Biome> biome) {
        this.biomes.add(biome);
        return this;
    }

    public DragonTypeBuilder setSneezeParticle(SimpleParticleType particle) {
        this.sneezeParticle = particle;
        return this;
    }

    public DragonTypeBuilder setEggParticle(SimpleParticleType particle) {
        this.eggParticle = particle;
        return this;
    }

    public DragonTypeBuilder setMaterial(TagKey<Item> material) {
        this.scales = material;
        return this;
    }

    public DragonTypeBuilder setScaleColor(MapColor color) {
        this.scaleColor = color;
        return this;
    }

    public <T extends DragonType> T register(
            BiFunction<ResourceLocation, DragonTypeBuilder, T> factory,
            ResourceLocation identifier
    ) {
        return factory.apply(identifier, this);
    }

    /// Sets the body + head GeckoLib geo model paths from their shape tokens.
    /// Body  -> assets/neodragonmounts/geo/model/dragonmounts2.dragon.&lt;bodyShape&gt;.geo.json
    /// Head  -> assets/neodragonmounts/geo/head/dragonmounts2.head_block.&lt;headShape&gt;.geo.json
    public DragonTypeBuilder model(String bodyShape, String headShape) {
        this.geoModel = makeId("geo/model/dragonmounts2.dragon." + bodyShape + ".geo.json");
        this.headGeoModel = makeId("geo/head/dragonmounts2.head_block." + headShape + ".geo.json");
        return this;
    }

    public DragonTypeBuilder texture(ResourceLocation texture) {
        this.texture = texture;
        return this;
    }

    /// Opts this type into GeckoLib armour rendering.
    /// Texture -> assets/neodragonmounts/textures/models/armor/geo/&lt;name&gt;.png
    /// Left unset, DragonScaleArmorItem hands back a null renderer and the type stays on
    /// vanilla's _layer_1/_layer_2 path. That is the intended state for types without art.
    public DragonTypeBuilder armor(String name) {
        this.armorTexture = makeId("textures/models/armor/geo/" + name + ".png");
        return this;
    }

    /// Overrides the geometry of a single slot -- a chestplate that carries wings, say.
    /// Slots left alone fall back to the shared dragon_scale set.
    /// -> assets/neodragonmounts/geo/armor/dragonmounts2.&lt;set&gt;.&lt;slot&gt;.geo.json
    public DragonTypeBuilder armorGeo(ArmorItem.Type slot, String set) {
        this.armorGeo.put(slot, makeId("geo/armor/dragonmounts2." + set + "." + slot.getName() + ".geo.json"));
        return this;
    }

    public DragonTypeBuilder addEffectImmunity(Holder<MobEffect> effect) {
        this.effectImmunities.add(effect);
        return this;
    }

}
