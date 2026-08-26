package net.dragonmounts.neo.common.init;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;

/**
 * 1.21.1 {@link ArmorMaterial} has no {@code durability} or {@code assetId} (those are 1.21.4).
 * Durability must be applied at item construction via {@code ArmorItem.Type.BODY.getDurability(factor)};
 * the 1.21.4 asset key becomes a {@link ArmorMaterial.Layer} in the {@code layers} list.
 */
public class DragonArmorMaterials {
    /// The dyeable tier, and the only one below copper.
    public static final ArmorMaterial LEATHER = makeMaterial(ArmorMaterials.LEATHER, 1);
    public static final ArmorMaterial COPPER;
    public static final ArmorMaterial IRON = makeMaterial(ArmorMaterials.IRON, 3);
    public static final ArmorMaterial GOLD = makeMaterial(ArmorMaterials.GOLD, 5);
    public static final ArmorMaterial EMERALD;
    public static final ArmorMaterial DIAMOND = makeMaterial(ArmorMaterials.DIAMOND, 9);
    public static final ArmorMaterial NETHERITE = makeMaterial(ArmorMaterials.NETHERITE, 11);

    static {
        var defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 1);
        defense.put(ArmorItem.Type.LEGGINGS, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 4);
        defense.put(ArmorItem.Type.HELMET, 2);
        defense.put(ArmorItem.Type.BODY, 2);
        COPPER = new ArmorMaterial(
                defense,
                8,
                SoundEvents.ARMOR_EQUIP_GENERIC,
                () -> Ingredient.of(Items.IRON_INGOT),   // was ItemTags.REPAIRS_CHAIN_ARMOR (no such tag in 1.21.1)
                List.of(new ArmorMaterial.Layer(withDefaultNamespace("copper"))),
                0.0F,
                0.0F
        );
    }

    static {
        var base = ArmorMaterials.DIAMOND.value();
        var defense = new EnumMap<>(base.defense());
        defense.put(ArmorItem.Type.BODY, 6);
        EMERALD = new ArmorMaterial(
                defense,
                base.enchantmentValue(),
                base.equipSound(),
                base.repairIngredient(),
                List.of(new ArmorMaterial.Layer(withDefaultNamespace("emerald"))),
                base.toughness(),
                base.knockbackResistance()
        );
    }

    public static ArmorMaterial makeMaterial(Holder<ArmorMaterial> base, int defense) {
        var value = base.value();
        var copy = new EnumMap<>(value.defense());
        copy.put(ArmorItem.Type.BODY, defense);
        return new ArmorMaterial(
                copy,
                value.enchantmentValue(),
                value.equipSound(),
                value.repairIngredient(),
                value.layers(),
                value.toughness(),
                value.knockbackResistance()
        );
    }


}