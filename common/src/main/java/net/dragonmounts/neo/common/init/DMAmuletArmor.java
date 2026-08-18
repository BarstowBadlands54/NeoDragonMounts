package net.dragonmounts.neo.common.init;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Map;
import java.util.List;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * Makes amulets wearable in the chest slot.
 * <p>
 * Going through {@link ArmorItem} rather than a custom render layer means vanilla's
 * HumanoidArmorLayer draws the amulet for free, and the second {@link ArmorMaterial.Layer} being
 * marked dyeable means the overlay picks up the stack's DYED_COLOR with no client code at all --
 * exactly how leather armour works.
 * <p>
 * Textures resolve to {@code textures/models/armor/<name>_layer_1.png} at 64x32, laid out on the
 * humanoid armour template (the torso front face is x20-27, y20-31).
 */
public final class DMAmuletArmor {
    /**
     * Half a point of armour. ArmorMaterial's defence map is Map&lt;Type,Integer&gt; and cannot
     * express 0.5, so the material contributes nothing and the value is applied as an explicit
     * attribute modifier instead. Passing this to Properties#attributes replaces the modifiers
     * ArmorItem would otherwise derive from the material.
     */
    public static final double ARMOR_POINTS = 0.5;
    private static final ResourceLocation MODIFIER_ID = makeId("amulet_armor");

    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(MODIFIER_ID, ARMOR_POINTS, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.CHEST)
                .build();
    }

    /**
     * @param name the texture stem, e.g. "aether_dragon_amulet" ->
     *             textures/models/armor/aether_dragon_amulet_layer_1.png
     */
    public static ArmorMaterial material(String name) {
        return new ArmorMaterial(
                Map.of(),                       // no defence from the material -- see attributes()
                0,                              // no enchantability
                SoundEvents.ARMOR_EQUIP_GENERIC,
                () -> Ingredient.EMPTY,         // not repairable
                List.of(
                        // body: untinted
                        new ArmorMaterial.Layer(makeId(name), "", false),
                        // overlay: dyeable, so DYED_COLOR tints it on the player
                        new ArmorMaterial.Layer(makeId("dragon_amulet_overlay"), "", true)
                ),
                0.0F,                           // toughness
                0.0F                            // knockback resistance
        );
    }

    private DMAmuletArmor() {}
}
