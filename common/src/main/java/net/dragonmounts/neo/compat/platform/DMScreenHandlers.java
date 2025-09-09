package net.dragonmounts.neo.compat.platform;

import net.dragonmounts.neo.common.inventory.DragonCoreHandler;
import net.dragonmounts.neo.common.inventory.DragonInventoryHandler;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.world.inventory.MenuType;

public class DMScreenHandlers {
    public static final MenuType<DragonCoreHandler> DRAGON_CORE = Dummy.get();
    public static final MenuType<DragonInventoryHandler> DRAGON_INVENTORY = Dummy.get();
}
