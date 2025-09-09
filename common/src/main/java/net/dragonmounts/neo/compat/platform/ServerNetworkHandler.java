package net.dragonmounts.neo.compat.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@SuppressWarnings("unused")
public class ServerNetworkHandler {
    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {}

    public static void sendTracking(Entity entity, CustomPacketPayload payload) {}

    public static void sendToAll(MinecraftServer server, CustomPacketPayload payload) {}
}
