package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.Item;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

public class DragonScalesItem extends Item implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scales";
    public final DragonType type;

    public DragonScalesItem(DragonType type, Properties props) {
        super(props.component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}
