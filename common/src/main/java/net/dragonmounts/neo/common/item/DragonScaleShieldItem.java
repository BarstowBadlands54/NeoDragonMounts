package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public class DragonScaleShieldItem extends ShieldItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_shield";
    public static final int UNIT_DURABILITY = 50;
    public final DragonType type;
    public final TranslatableContents name;

    public DragonScaleShieldItem(DragonType type, Properties props) {
        super(props.component(DMDataComponents.DRAGON_TYPE, type)
                .durability(type.tier.getUses())
        );
        this.type = type;
        this.name = new TranslatableContents(TRANSLATION_KEY + ".name", null, new Object[]{MutableComponent.create(type.name)});
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return this.type.material.repairIngredient().get().test(repair);
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