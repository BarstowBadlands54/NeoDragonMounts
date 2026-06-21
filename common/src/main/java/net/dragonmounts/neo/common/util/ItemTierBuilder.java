package net.dragonmounts.neo.common.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;

public class ItemTierBuilder {
    public final TagKey<Block> incorrectBlocks;
    public final int durability;
    public final float speed;
    public final float damage;
    public int enchantmentValue = 1;

    public ItemTierBuilder(TagKey<Block> incorrectBlocks, int durability, float speed, float damage) {
        this.incorrectBlocks = incorrectBlocks;
        this.durability = durability;
        this.speed = speed;
        this.damage = damage;
    }

    public ItemTierBuilder setEnchantmentValue(int enchantmentValue) {
        this.enchantmentValue = enchantmentValue;
        return this;
    }

    @Contract("null -> fail")
    public Tier build(TagKey<Item> ingredient) {
        if (ingredient == null) throw new IllegalArgumentException();
        return new Tier() {
            @Override public int getUses() { return durability; }
            @Override public float getSpeed() { return speed; }
            @Override public float getAttackDamageBonus() { return damage; }
            @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrectBlocks; }
            @Override public int getEnchantmentValue() { return enchantmentValue; }
            @Override public Ingredient getRepairIngredient() { return Ingredient.of(ingredient); }
        };
    }
}
