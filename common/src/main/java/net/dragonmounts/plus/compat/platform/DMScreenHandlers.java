package net.dragonmounts.plus.compat.platform;

import net.dragonmounts.plus.common.inventory.DragonCoreHandler;
import net.dragonmounts.plus.common.inventory.DragonInventoryHandler;
import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.world.inventory.MenuType;

public class DMScreenHandlers {
    public static final MenuType<DragonCoreHandler> DRAGON_CORE = Dummy.get();
    public static final MenuType<DragonInventoryHandler> DRAGON_INVENTORY = Dummy.get();
}
