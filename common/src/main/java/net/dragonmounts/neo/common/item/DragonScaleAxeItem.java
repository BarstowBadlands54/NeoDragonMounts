package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

public class DragonScaleAxeItem extends AxeItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_axe";
    public final DragonType type;
    public final TranslatableContents name;

    public DragonScaleAxeItem(DragonType type, float damage, float speed, Properties props) {
        super(type.tier, props.component(DMDataComponents.DRAGON_TYPE, type)
                .attributes(DiggerItem.createAttributes(type.tier, damage, speed)));
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