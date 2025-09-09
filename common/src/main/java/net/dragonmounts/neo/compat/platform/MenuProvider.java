package net.dragonmounts.neo.compat.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface MenuProvider<D> extends net.minecraft.world.MenuProvider {
    void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer);

    D getScreenOpeningData(ServerPlayer player);
}