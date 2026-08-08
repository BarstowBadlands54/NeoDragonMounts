package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public class DragonScaleBowItem extends BowItem implements DragonTypified {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_scale_bow";
    public final DragonType type;
    public final TranslatableContents name;

    /// Defaults to this family's own translation key.
    public DragonScaleBowItem(DragonType type, Properties props) {
        this(type, TRANSLATION_KEY, props);
    }

    public DragonScaleBowItem(DragonType type, String translationKey, Properties props) {
        super(props.component(DMDataComponents.DRAGON_TYPE, type)
                .durability(type.tier.getUses() >> 1)
        );
        this.type = type;
        this.name = new TranslatableContents(translationKey + ".name", null, new Object[]{MutableComponent.create(type.name)});
    }

    @Override
    public int getEnchantmentValue() {
        return this.type.tier.getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return this.type.tier.getRepairIngredient().test(repairCandidate);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        super.shootProjectile(shooter, projectile, index, velocity * 1.25F, inaccuracy * 0.75F, angle, target);
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