package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DescribedArmorEffect;
import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class DragonScaleArmorItem extends ArmorItem implements DragonTypified {
    public final DragonType type;
    public final DescribedArmorEffect effect;

    public DragonScaleArmorItem(DragonType type, DescribedArmorEffect effect, ArmorItem.Type slot, Properties props) {
        // 1.21.1 ArmorItem takes Holder<ArmorMaterial>; type.material is a raw ArmorMaterial -> wrap it.
        super(Holder.direct(type.material), slot, props.component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
        this.effect = effect;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(this.effect);
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }
}