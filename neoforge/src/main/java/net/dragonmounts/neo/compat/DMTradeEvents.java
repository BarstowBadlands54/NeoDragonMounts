package net.dragonmounts.neo.compat;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.trade.DragonHeadForEmeralds;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/**
 * Adds the dragon-head listing to the wandering trader's rare pool.
 * <p>
 * {@code WandererTradesEvent} is a plain {@code Event}, not an {@code IModBusEvent}, so it
 * arrives on the game bus — which is where {@code @EventBusSubscriber} puts non-mod-bus
 * handlers in NeoForge 1.21.1. The annotation has no {@code bus} parameter in this version.
 */
@EventBusSubscriber(modid = DragonMountsShared.NAMESPACE)
public final class DMTradeEvents {
    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        // getRareTrades(), not getRare(). One listing is drawn from this pool per trader.
        event.getRareTrades().add(new DragonHeadForEmeralds());
    }

    private DMTradeEvents() {}
}
