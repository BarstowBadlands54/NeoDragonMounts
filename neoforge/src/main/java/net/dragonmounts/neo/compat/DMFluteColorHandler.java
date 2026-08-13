package net.dragonmounts.neo.compat;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.item.FluteItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints the dyeable overlay on every flute variety.
 * <p>
 * Crafting a flute with a dye already worked — {@code ArmorDyeRecipe} only requires the item to
 * be in {@code minecraft:dyeable}, and it writes a {@code DYED_COLOR} component. Nothing was
 * reading that component back at render time, so layer 1 drew in its raw greyscale and the flute
 * looked unchanged.
 * <p>
 * Layer 0 is the flute body and returns -1 (no tint). Layer 1 is the overlay and takes the dye.
 * Default is white so an undyed flute looks exactly as it does now.
 */
@EventBusSubscriber(modid = DragonMountsShared.NAMESPACE, value = Dist.CLIENT)
public final class DMFluteColorHandler {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof FluteItem) {
                event.register(
                        (stack, tintIndex) -> tintIndex == 0
                                ? -1
                                : DyedItemColor.getOrDefault(stack, 0xFFFFFF),
                        item);
            }
        }
    }

    private DMFluteColorHandler() {}
}
