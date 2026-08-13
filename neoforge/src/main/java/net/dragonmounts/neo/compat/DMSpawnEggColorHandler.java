package net.dragonmounts.neo.compat;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.client.DMSpawnEggColors;
import net.dragonmounts.neo.common.item.DragonSpawnEggItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints the dragon spawn eggs.
 * <p>
 * Registers one provider against every {@code DragonSpawnEggItem} in the registry rather than a
 * hand-written list, so a breed added later is covered without editing this class.
 * <p>
 * Client-only: {@code Dist.CLIENT} on the annotation keeps it off dedicated servers.
 */
@EventBusSubscriber(modid = DragonMountsShared.NAMESPACE, value = Dist.CLIENT)
public final class DMSpawnEggColorHandler {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof DragonSpawnEggItem) {
                event.register((stack, tintIndex) -> DMSpawnEggColors.getColor(stack, tintIndex), item);
            }
        }
    }

    private DMSpawnEggColorHandler() {}
}
