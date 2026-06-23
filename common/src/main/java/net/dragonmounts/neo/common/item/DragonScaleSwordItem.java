package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.SwordItem;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

public class DragonScaleSwordItem extends SwordItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_sword";
    public final DragonType type;

    public DragonScaleSwordItem(DragonType type, int damage, float speed, Properties props) {
        super(type.tier, props
                .component(DMDataComponents.DRAGON_TYPE, type)
                .attributes(SwordItem.createAttributes(type.tier, damage, speed)));
        this.type = type;
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}