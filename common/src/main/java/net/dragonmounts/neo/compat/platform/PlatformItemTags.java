package net.dragonmounts.neo.compat.platform;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public interface PlatformItemTags {
    TagKey<Item> WOODEN_CHESTS = Dummy.get();
}
