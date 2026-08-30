package net.dragonmounts.neo.common.init;

import net.dragonmounts.neo.common.crafting.DragonArmorUpgradeRecipe;
import net.dragonmounts.neo.common.crafting.DragonScaleUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
import static net.dragonmounts.neo.compat.registry.RegistryHandler.registerRecipe;

public interface DMRecipes {
    RecipeSerializer<DragonArmorUpgradeRecipe> DRAGON_ARMOR_UPGRADE = registerRecipe(
            makeId("smithing_special_dragon_armor_upgrade"),
            new DragonArmorUpgradeRecipe.Serializer()
    );

    RecipeSerializer<DragonScaleUpgradeRecipe> DRAGON_SCALE_UPGRADE = registerRecipe(
            makeId("smithing_special_dragon_scale_upgrade"),
            new DragonScaleUpgradeRecipe.Serializer()
    );

    static void init() {}
}
