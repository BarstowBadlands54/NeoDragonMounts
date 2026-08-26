package net.dragonmounts.neo.client;

import net.dragonmounts.neo.common.client.DMItemColors;
import net.dragonmounts.neo.common.init.DMItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = "neodragonmounts", value = Dist.CLIENT)
public final class DMItemColorHandler {
    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(DMItemColors::dyeableArmor, DMItems.LEATHER_DRAGON_ARMOR);
    }

    private DMItemColorHandler() {}
}