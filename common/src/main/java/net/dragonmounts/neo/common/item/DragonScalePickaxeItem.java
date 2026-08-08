package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.PickaxeItem;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

public class DragonScalePickaxeItem extends PickaxeItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_pickaxe";
    public final DragonType type;
    public final TranslatableContents name;

    /// Defaults to this family's own translation key.
    public DragonScalePickaxeItem(DragonType type, float damage, float speed, Properties props) {
        this(type, TRANSLATION_KEY, damage, speed, props);
    }

    public DragonScalePickaxeItem(DragonType type, String translationKey, float damage, float speed, Properties props) {
        super(type.tier, props.component(DMDataComponents.DRAGON_TYPE, type)
                .attributes(DiggerItem.createAttributes(type.tier, damage, speed)));
        this.type = type;
        this.name = new TranslatableContents(translationKey + ".name", null, new Object[]{MutableComponent.create(type.name)});
    }


    @Override
    public Component getName(ItemStack stack) {
        return MutableComponent.create(this.name);
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}