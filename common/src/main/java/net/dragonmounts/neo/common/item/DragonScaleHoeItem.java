package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.HoeItem;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

public class DragonScaleHoeItem extends HoeItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_hoe";
    public final DragonType type;

    public DragonScaleHoeItem(DragonType type, float damage, float speed, Properties props) {
        super(type.tier, damage, speed, props.component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}