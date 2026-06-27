package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.Item;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

public class DragonScalesItem extends Item implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scales";
    public final DragonType type;
    public final TranslatableContents name;

    public DragonScalesItem(DragonType type, Properties props) {
        super(props.component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
        this.name = new TranslatableContents(TRANSLATION_KEY + ".name", null, new Object[]{MutableComponent.create(type.name)});
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