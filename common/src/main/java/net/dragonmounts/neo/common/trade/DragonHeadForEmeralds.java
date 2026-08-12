package net.dragonmounts.neo.common.trade;

import net.dragonmounts.neo.common.init.DragonVariants;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

/**
 * Sells a randomly chosen dragon head for emeralds.
 * <p>
 * One listing rather than 63: {@code getOffer} is called fresh each time a trader spawns and
 * rolls its stock, so a single instance already produces a different head per trader. Adding
 * 63 separate listings would instead make dragon heads crowd out every other rare trade.
 * <p>
 * Deliberately loader-agnostic — {@link VillagerTrades.ItemListing} is vanilla, so each loader
 * only has to hand this instance to its own trade-registration hook.
 */
public class DragonHeadForEmeralds implements VillagerTrades.ItemListing {
    public static final int MIN_PRICE = 12;
    public static final int MAX_PRICE = 24;
    public static final int MAX_USES = 1;
    public static final int XP = 1;
    public static final float PRICE_MULTIPLIER = 0.05F;

    @Override
    public @Nullable MerchantOffer getOffer(Entity trader, RandomSource random) {
        var variants = DragonVariants.BUILTIN_VALUES;
        if (variants.isEmpty()) return null;   // registry not populated yet; skip rather than crash
        var variant = variants.get(random.nextInt(variants.size()));
        var head = new ItemStack(variant.head.item.get());
        int price = MIN_PRICE + random.nextInt(MAX_PRICE - MIN_PRICE + 1);
        return new MerchantOffer(new ItemCost(Items.EMERALD, price), head, MAX_USES, XP, PRICE_MULTIPLIER);
    }
}
