package net.dragonmounts.neo.common.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;

public class ArmorMaterialBuilder {
    public final EnumMap<EquipmentSlot, Integer> defense = new EnumMap<>(EquipmentSlot.class);
    public int durabilityFactor;
    public int enchantmentValue = 1;
    public Holder<SoundEvent> sound = SoundEvents.ARMOR_EQUIP_GOLD;
    public float toughness = 0;
    public float knockbackResistance = 0;

    public ArmorMaterialBuilder(int durabilityFactor) {
        this.setDurabilityFactor(durabilityFactor).setDefense(EquipmentSlot.BODY, 11);
    }

    public ArmorMaterialBuilder setDurabilityFactor(int durabilityFactor) {
        this.durabilityFactor = durabilityFactor;
        return this;
    }

    public ArmorMaterialBuilder setDefense(EquipmentSlot type, int defense) {
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
            @NotNull ResourceKey<EquipmentAsset> asset
    ) {
        if (ingredient == null) throw new IllegalArgumentException();
        return new ArmorMaterial(
                this.durabilityFactor,
                new EnumMap<>(this.defense),
                this.enchantmentValue,
                this.sound,
                this.toughness,
                this.knockbackResistance,
                ingredient
        );
    }
}
