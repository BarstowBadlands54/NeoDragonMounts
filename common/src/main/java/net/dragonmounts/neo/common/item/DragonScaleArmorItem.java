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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

public class DragonScaleArmorItem extends ArmorItem implements DragonTypified {
    public final DragonType type;
    public final DescribedArmorEffect effect;
    public final TranslatableContents name;

    /// Defaults to the "dragon_scale_" family prefix.
    public DragonScaleArmorItem(DragonType type, DescribedArmorEffect effect, Type slot, Properties props) {
        this(type, effect, "dragon_scale_", slot, props);
    }

    public DragonScaleArmorItem(DragonType type, DescribedArmorEffect effect, String keyPrefix, Type slot, Properties props) {
        super(Holder.direct(type.material),
                slot,
                props.durability(slot.getDurability(DURABILITY_FACTOR))   // ← durability + implicit stacksTo(1)
                        .component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
        this.effect = effect;
        this.name = new TranslatableContents(ITEM_TRANSLATION_KEY_PREFIX + keyPrefix + slot.getName() + ".name", null, new Object[]{MutableComponent.create(type.name)});
    }

    private static final int DURABILITY_FACTOR = 33;   // diamond-tier; tune per balance
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(this.effect);
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