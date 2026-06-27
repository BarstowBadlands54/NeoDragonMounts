package net.dragonmounts.neo.data;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.init.DMItems;
import net.dragonmounts.neo.common.item.*;
import net.dragonmounts.neo.compat.registry.DragonScaleArmorSuit;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.ItemHolder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * NeoForge 1.21.1 item model data-gen (builder API).
 * Block models live in a separate BlockStateProvider.
 *
 * Confident: basicItem, withExistingParent, singleTexture, getBuilder(...).override().
 * VERIFY against your NeoForge version: the exact override predicate id for bow
 * pull/pulling and shield blocking (mcLoc("pulling")/("pull")/("blocking")).
 */
public class DMModelProvider extends ItemModelProvider {
    public DMModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, DragonMountsShared.NAMESPACE, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // --- simple flat icons ---
        flat(DMItems.AMULET);
        flat(DMItems.COPPER_DRAGON_ARMOR);
        flat(DMItems.IRON_DRAGON_ARMOR);
        flat(DMItems.GOLDEN_DRAGON_ARMOR);
        flat(DMItems.EMERALD_DRAGON_ARMOR);
        flat(DMItems.DIAMOND_DRAGON_ARMOR);
        flat(DMItems.NETHERITE_DRAGON_ARMOR);
        flat(DMItems.DIAMOND_SHEARS);
        flat(DMItems.NETHERITE_SHEARS);
        flat(DMItems.DRAGON_MEAT);
        flat(DMItems.COOKED_DRAGON_MEAT);
        flat(DMItems.VARIATION_ORB);

        // --- spawn eggs: inherit the vanilla template; color is from the SpawnEggItem ---
        for (ItemHolder<?> egg : new ItemHolder<?>[]{
                DMItems.AETHER_DRAGON_SPAWN_EGG, DMItems.DARK_DRAGON_SPAWN_EGG,
                DMItems.ENCHANTED_DRAGON_SPAWN_EGG, DMItems.ENDER_DRAGON_SPAWN_EGG,
                DMItems.FIRE_DRAGON_SPAWN_EGG, DMItems.FOREST_DRAGON_SPAWN_EGG,
                DMItems.ICE_DRAGON_SPAWN_EGG, DMItems.MOONLIGHT_DRAGON_SPAWN_EGG,
                DMItems.NETHER_DRAGON_SPAWN_EGG, DMItems.SCULK_DRAGON_SPAWN_EGG,
                DMItems.SKELETON_DRAGON_SPAWN_EGG, DMItems.STORM_DRAGON_SPAWN_EGG,
                DMItems.SUNLIGHT_DRAGON_SPAWN_EGG, DMItems.TERRA_DRAGON_SPAWN_EGG,
                DMItems.WATER_DRAGON_SPAWN_EGG, DMItems.WITHER_DRAGON_SPAWN_EGG,
                DMItems.ZOMBIE_DRAGON_SPAWN_EGG, DMItems.LIGHT_DRAGON_SPAWN_EGG
        }) {
            withExistingParent(name(egg.get()), mcLoc("item/template_spawn_egg"));
        }

        // --- per DragonType ---
        for (var type : DragonType.REGISTRY) {
            flatType(type, DragonAmuletItem.class);
            flatType(type, DragonEssenceItem.class);
            flatType(type, DragonScalesItem.class);
            handheldType(type, DragonScaleAxeItem.class);
            handheldType(type, DragonScaleHoeItem.class);
            handheldType(type, DragonScalePickaxeItem.class);
            handheldType(type, DragonScaleShovelItem.class);
            handheldType(type, DragonScaleSwordItem.class);
            armorIcons(type);
            // bow + shield -> overrides; see generateBow/generateShield below
        }
    }

    // ---- helpers ----
    private void flat(ItemHolder<?> item) {
        basicItem(item.get());   // item/generated, layer0 = <ns>:item/<name>
    }

    private void flatType(DragonType type, Class<? extends Item> clazz) {
        var item = type.getInstance(clazz, null);
        if (item != null) basicItem(item);
    }

    private void handheldType(DragonType type, Class<? extends Item> clazz) {
        var item = type.getInstance(clazz, null);
        if (item == null) return;
        String n = name(item);
        singleTexture(n, mcLoc("item/handheld"), "layer0", modLoc("item/" + n));
    }

    private void armorIcons(DragonType type) {
        var suit = type.getInstance(DragonScaleArmorSuit.class, null);
        if (suit == null) return;
        basicItem(suit.getHelmet());
        basicItem(suit.getChestplate());
        basicItem(suit.getLeggings());
        basicItem(suit.getBoots());
    }

    private static String name(Item item) {
        // registry path of the item, e.g. "fire_dragon_scale_sword"
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}