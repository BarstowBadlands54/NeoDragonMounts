package net.dragonmounts.neo.common.client;

import net.dragonmounts.neo.common.init.DMItems;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;


public final class DMItemColors {
    private static final int DYED_LAYER = 0;
    private static final int NO_TINT = -1;


    public static int dyeableArmor(ItemStack stack, int layer) {
        return layer == DYED_LAYER
                ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, DMItems.UNDYED_LEATHER))
                : NO_TINT;
    }

    private DMItemColors() {}
}
