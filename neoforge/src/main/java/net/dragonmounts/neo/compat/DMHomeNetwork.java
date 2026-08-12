package net.dragonmounts.neo.compat;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.item.FluteItem;
import net.dragonmounts.neo.common.network.c2s.DragonHomePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Registers the flute's home payload.
 * <p>
 * Self-contained on purpose: {@code RegisterPayloadHandlersEvent} implements
 * {@code IModBusEvent}, so {@code @EventBusSubscriber} routes it to the mod bus without a
 * {@code bus} parameter (that parameter does not exist in NeoForge 1.21.1), and
 * {@code registrar(String)} returns a fresh {@code PayloadRegistrar} per call — so this can
 * register alongside the mod's existing network class without touching it or colliding with it.
 * <p>
 * The version string below is intentionally its own. Channels are keyed per registrar, and a
 * payload only has to live in <em>some</em> registered channel to pass
 * {@code NetworkRegistry.checkPacket}.
 */
@EventBusSubscriber(modid = DragonMountsShared.NAMESPACE)
public final class DMHomeNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("dm-home-1").playToServer(
                DragonHomePayload.TYPE,
                DragonHomePayload.CODEC,
                (payload, context) -> {
                    // playToServer only ever runs server-side, so this cast is safe
                    var player = (ServerPlayer) context.player();
                    if (payload.recall()) {
                        FluteItem.recallHome(player, payload.dragon());
                    } else {
                        FluteItem.setHome(player, payload.dragon());
                    }
                });
    }

    private DMHomeNetwork() {}
}
