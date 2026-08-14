package net.dragonmounts.neo;

import net.dragonmounts.neo.common.item.FluteItem;
import net.dragonmounts.neo.common.network.c2s.DragonHomePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Fabric half of the flute home payload.
 * <p>
 * A separate {@link ModInitializer} so nothing in the existing {@code DragonMounts} class has to
 * change — add it as a second {@code main} entrypoint in {@code fabric.mod.json}:
 *
 * <pre>
 * "entrypoints": {
 *   "main": [
 *     "net.dragonmounts.neo.DragonMounts",
 *     "net.dragonmounts.neo.DMHomeNetworkFabric"
 *   ]
 * }
 * </pre>
 *
 * If you would rather not add an entrypoint, delete this class and instead call
 * {@link #registerHomePayload()} from {@code DragonMounts.initNetwork()}.
 */
public class DMHomeNetworkFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        registerHomePayload();
    }

    public static void registerHomePayload() {
        PayloadTypeRegistry.playC2S().register(DragonHomePayload.TYPE, DragonHomePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DragonHomePayload.TYPE, (payload, context) -> {
            var player = context.player();
            if (payload.recall()) {
                FluteItem.recallHome(player, payload.dragon());
            } else {
                FluteItem.setHome(player, payload.dragon(), player.getOnPos());
            }
        });
    }
}
