package net.dragonmounts.neo.common.util;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;
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
        return new SimpleTier(
                this.incorrectBlocks,        // TagKey<Block> incorrectBlocksForDrops
                this.durability,             // int uses
                this.speed,                  // float speed
                this.damage,                 // float attackDamageBonus
                this.enchantmentValue,       // int enchantmentValue
                () -> Ingredient.of(ingredient)   // Supplier<Ingredient> repairIngredient
        );
    }
}
