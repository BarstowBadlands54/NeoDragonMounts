package net.dragonmounts.neo.compat;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.item.AmuletItem;
import net.dragonmounts.neo.common.item.FluteItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints the dyeable overlay on amulets and flutes.
 * <p>
 * Layer 0 is the item body and returns -1 (untinted). Layer 1 is the overlay and takes the dye.
 * <p>
 * The 0xFF000000 is not optional: 1.21.1 reads item tints as ARGB — ItemRenderer pulls the alpha
 * byte out via FastColor.ARGB32.alpha() — so a plain 0xRRGGBB has alpha 0 and the quad renders
 * fully transparent. That is what made the spawn eggs invisible rather than merely untinted.
 * DyedItemColor.getOrDefault returns RGB, so the alpha has to be added here.
 */
@EventBusSubscriber(modid = DragonMountsShared.NAMESPACE, value = Dist.CLIENT)
public final class DMDyeableColorHandler {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof AmuletItem<?> || item instanceof FluteItem) {
                event.register(
                        (stack, tintIndex) -> tintIndex == 0
                                ? -1
                                : 0xFF000000 | DyedItemColor.getOrDefault(stack, 0xFFFFFF),
                        item);
            }
        }
    }

    private DMDyeableColorHandler() {}
}
