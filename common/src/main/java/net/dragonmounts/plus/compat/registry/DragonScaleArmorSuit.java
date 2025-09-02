package net.dragonmounts.plus.compat.registry;

import net.dragonmounts.plus.common.api.ArmorEffectSource;
import net.dragonmounts.plus.common.api.DescribedArmorEffect;
import net.dragonmounts.plus.common.api.DragonTypified;
import net.dragonmounts.plus.common.capability.ArmorEffectManager;
import net.dragonmounts.plus.common.item.DragonScaleArmorItem;
import net.dragonmounts.plus.common.util.ArmorSuitInfo;
import net.dragonmounts.plus.common.util.ItemGroup;
import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static net.dragonmounts.plus.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

@SuppressWarnings("ClassCanBeRecord")
public final class DragonScaleArmorSuit implements DragonTypified, ArmorEffectSource {
    public static final String HELMET_TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_helmet";
    public static final String CHESTPLATE_TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_chestplate";
    public static final String LEGGINGS_TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_leggings";
    public static final String BOOTS_TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_boots";

    public static DragonScaleArmorSuit makeSuit(
            DragonType type,
            DescribedArmorEffect effect,
            ItemGroup group,
            String helmet,
            String chestplate,
            String leggings,
            String boots,
            ArmorSuitInfo.Factory<DragonScaleArmorSuit, DragonScaleArmorItem> factory
    ) {
        return Dummy.get();
    }

    public final DragonType type;
    public final DescribedArmorEffect effect;
    public final ArmorSuitInfo<DragonScaleArmorSuit, DragonScaleArmorItem> info;

    public DragonScaleArmorSuit(
            ArmorSuitInfo<DragonScaleArmorSuit, DragonScaleArmorItem> info,
            DragonType type,
            DescribedArmorEffect effect
    ) {
        this.info = info;
        this.type = type;
        this.effect = effect;
    }

    public DragonScaleArmorItem getHelmet() {
        return Dummy.get();
    }

    public DragonScaleArmorItem getChestplate() {
        return Dummy.get();
    }

    public DragonScaleArmorItem getLeggings() {
        return Dummy.get();
    }

    public DragonScaleArmorItem getBoots() {
        return Dummy.get();
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }

    @Override
    public void affect(ArmorEffectManager manager, Player player, ItemStack stack) {}

    @Override
    public ArmorEffectSourceType<?> getType() {
        return ArmorEffectSourceType.BUILTIN;
    }
}
