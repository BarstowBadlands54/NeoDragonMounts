package net.dragonmounts.neo.common.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;

public class ArmorMaterialBuilder {
    public final EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
    public int durabilityFactor;
    public int enchantmentValue = 1;
    public Holder<SoundEvent> sound = SoundEvents.ARMOR_EQUIP_GOLD;
    public float toughness = 0;
    public float knockbackResistance = 0;

    public ArmorMaterialBuilder(int durabilityFactor) {
        this.setDurabilityFactor(durabilityFactor).setDefense(ArmorItem.Type.BODY, 11);
    }

    public ArmorMaterialBuilder setDurabilityFactor(int durabilityFactor) {
        this.durabilityFactor = durabilityFactor;
        return this;
    }

    public ArmorMaterialBuilder setDefense(ArmorItem.Type type, int defense) {
        this.defense.put(type, defense);
        return this;
    }

    public ArmorMaterialBuilder setEnchantmentValue(int enchantmentValue) {
        this.enchantmentValue = enchantmentValue;
        return this;
    }

    public ArmorMaterialBuilder setSound(Holder<SoundEvent> sound) {
        this.sound = sound;
        return this;
    }

    public ArmorMaterialBuilder setToughness(float toughness) {
        this.toughness = toughness;
        return this;
    }

    public ArmorMaterialBuilder setKnockbackResistance(float knockbackResistance) {
        this.knockbackResistance = knockbackResistance;
        return this;
    }

    @Contract("null, _ -> fail")
    public ArmorMaterial build(
            TagKey<Item> ingredient,
            @NotNull ResourceLocation assetName
    ) {
        if (ingredient == null) throw new IllegalArgumentException();
        return new ArmorMaterial(
                new EnumMap<>(this.defense),                  // Map<ArmorItem.Type, Integer> defense
                this.enchantmentValue,                        // int enchantmentValue
                this.sound,                                   // Holder<SoundEvent> equipSound
                () -> Ingredient.of(ingredient),              // Supplier<Ingredient> repairIngredient
                List.of(new ArmorMaterial.Layer(assetName)),  // List<ArmorMaterial.Layer> layers
                this.toughness,                               // float toughness
                this.knockbackResistance                      // float knockbackResistance
        );
    }
}