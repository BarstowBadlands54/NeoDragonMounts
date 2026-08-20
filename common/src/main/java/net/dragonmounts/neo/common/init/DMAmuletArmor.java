package net.dragonmounts.neo.common.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * Makes amulets wearable in the chest slot.
 * <p>
 * Going through {@link ArmorItem} rather than a custom render layer means vanilla's
 * {@code HumanoidArmorLayer} draws the amulet for free, and marking the second
 * {@link ArmorMaterial.Layer} dyeable means the overlay picks up the stack's DYED_COLOR with no
 * client code at all -- exactly how leather armour works. Note that HumanoidArmorLayer already
 * wraps the tint in {@code FastColor.ARGB32.opaque(...)}, so the missing-alpha-byte trap that bit
 * the item-model tints does not apply on this path.
 * <p>
 * Textures resolve to {@code textures/models/armor/<name>_layer_1.png} at 64x32, laid out on the
 * humanoid armour template (the torso front face is x20-27, y20-31). {@code _layer_2} is only
 * consulted for the LEGS slot, so the chest slot never reads it.
 */
public final class DMAmuletArmor {
    /**
     * One armour point, i.e. half an armour icon on the HUD.
     * <p>
     * This cannot be 0.5. {@code LivingEntity#getArmorValue()} is
     * {@code Mth.floor(getAttributeValue(Attributes.ARMOR))} and the damage formula consumes that
     * int, so a fractional modifier is truncated to zero -- it would show nothing on the HUD and
     * reduce nothing. One is the smallest value Minecraft can actually represent.
     */
    public static final int ARMOR_POINTS = 1;

    /**
     * The tint used when a stack carries no dye.
     * <p>
     * HumanoidArmorLayer falls back to {@code DyedItemColor.getOrDefault(stack, -6265536)} --
     * vanilla's leather brown -- when DYED_COLOR is absent. The overlay art here is neutral grey
     * meant to be multiplied, so an undyed amulet would render a brown chain on the body while its
     * inventory icon stayed grey. Shipping a default component of white makes the multiply an
     * identity and keeps the two in step.
     */
    public static final int UNDYED_TINT = 0xFFFFFF;

    /** Texture stem shared by every amulet's dyeable layer. */
    public static final String OVERLAY = "dragon_amulet_overlay";

    /** Attaches the default tint; see {@link #UNDYED_TINT}. */
    public static Item.Properties withDefaultTint(Item.Properties props) {
        return props.component(DataComponents.DYED_COLOR, new DyedItemColor(UNDYED_TINT, false));
    }

    /**
     * @param texture the texture stem, e.g. "aether_dragon_amulet" ->
     *                textures/models/armor/aether_dragon_amulet_layer_1.png
     */
    public static ArmorMaterial material(String texture) {
        return new ArmorMaterial(
                // Defence lives in the material, not in an explicit Properties#attributes call:
                // ArmorItem builds its own ItemAttributeModifiers from this map and returns them
                // from getDefaultAttributeModifiers(), so a hand-rolled modifier would either be
                // shadowed or stack on top of the derived one. This also gets us vanilla's
                // "minecraft:armor.chestplate" modifier id and the CHEST slot group for free.
                Map.of(ArmorItem.Type.CHESTPLATE, ARMOR_POINTS),
                0,                              // enchantability: amulets take no Protection
                SoundEvents.ARMOR_EQUIP_CHAIN,
                () -> Ingredient.EMPTY,         // not repairable; amulets carry no durability
                List.of(
                        new ArmorMaterial.Layer(makeId(texture), "", false), // body, untinted
                        new ArmorMaterial.Layer(makeId(OVERLAY), "", true)   // chain, dyed
                ),
                0.0F,                           // toughness
                0.0F                            // knockback resistance
        );
    }

    private DMAmuletArmor() {}
}
